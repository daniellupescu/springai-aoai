package com.example.springai.aoai.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.example.springai.aoai.dto.ChatMessageDto;
import com.example.springai.aoai.exception.TooManyRequestsException;
import com.example.springai.aoai.mapper.ChatMessageMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.ModelType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.azure.openai.AzureOpenAiChatClient;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Chatbot service
 */
@Service
@Slf4j
public class ChatbotService {

    @Autowired
    private final ChatClient chatClient;

    @Autowired
    private final SystemMessage systemMessage;

    @Autowired
    private final ChatMessageMapper chatMessageMapper;

    @Value("${spring.ai.azure.openai.endpoint}")
    private String azureOpenAiEndpoint;

    @Value("${spring.ai.azure.openai.api-key}")
    private String azureOpenAiApiKey;

    @Value("${spring.ai.azure.openai.chat.options.model}")
    private String model;

    @Value("${spring.ai.azure.openai.chat.options.max-tokens}")
    private Integer maxTokens;

    @Value("${spring.ai.azure.openai.chat.options.temperature}")
    private Float temperature;

    private Encoding encoding;

    private Optional<ModelType> modelType;

    /**
     * Constructor
     *
     * @param chatClient        the chatClient
     * @param systemMessage     the system message
     * @param chatMessageMapper the ChatMessage Mapper
     */
    public ChatbotService(ChatClient chatClient, SystemMessage systemMessage, ChatMessageMapper chatMessageMapper) {
        this.chatClient = chatClient;
        this.systemMessage = systemMessage;
        this.chatMessageMapper = chatMessageMapper;
    }

    /**
     * Calls OpenAI chat completion API and returns a Generation object
     * Retry with exponential backoff on 429 Too Many Requests status code
     *
     * @param chatMessageDtos list of all user and assistant messages
     * @return a Generation object
     */
    @Retryable(
            retryFor = {TooManyRequestsException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, maxDelay = 5000, multiplier = 2))
    public Optional<Generation> completion(List<ChatMessageDto> chatMessageDtos) {
        List<Message> messages = new ArrayList<>(chatMessageMapper.toMessage(chatMessageDtos));
        messages.add(0, systemMessage);
        Prompt prompt = new Prompt(messages);
        Generation gen = null;

        try {
            gen = chatClient.call(prompt).getResults().get(0);
        } catch (RuntimeException e) {
            log.error("Caught a RuntimeException: " + e.getMessage());
            if (e instanceof HttpClientErrorException) {
                if (((HttpClientErrorException) e).getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
                    throw new TooManyRequestsException(e.getMessage());
                }
            }
        }
        return Optional.ofNullable(gen);
    }


    /**
     * Calls OpenAI chat completion API in Stream mode and returns a Flux object
     *
     * @param chatMessageDtos list of all user and assistant messages
     * @return a Generation object
     */
    @Retryable(
            retryFor = {TooManyRequestsException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, maxDelay = 5000, multiplier = 2))
    public Flux<Generation> completionWithStream(List<ChatMessageDto> chatMessageDtos) {
        List<Message> messages = new ArrayList<>(chatMessageMapper.toMessage(chatMessageDtos));
        messages.add(0, systemMessage);
        Prompt prompt = new Prompt(messages);

        OpenAIClientBuilder openAIClientBuilder = new OpenAIClientBuilder();
        OpenAIClient openAIClient = openAIClientBuilder.credential(new AzureKeyCredential(azureOpenAiApiKey)).endpoint(azureOpenAiEndpoint).buildClient();

        var azureOpenAiChatClient = new AzureOpenAiChatClient(openAIClient).withDefaultOptions(AzureOpenAiChatOptions.builder().withModel(model).withTemperature(temperature).withMaxTokens(maxTokens).build());

        return azureOpenAiChatClient.stream(prompt)
                .onErrorResume(e -> {
                    log.error("Caught a RuntimeException: " + e.getMessage());
                    if (e instanceof HttpClientErrorException) {
                        if (((HttpClientErrorException) e).getStatusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
                            throw new TooManyRequestsException(e.getMessage());
                        }
                    }
                    return Flux.empty();
                })
                .flatMap(s -> Flux.fromIterable(s.getResults()));
    }

    /**
     * Initializing encoding and model type after bean creation
     */
    @PostConstruct
    public void postConstructInit() {
        //Hack because jtokkit's model name has a dot
        this.modelType = ModelType.fromName(model.replace("35", "3.5"));
        if (this.modelType.isEmpty()) {
            log.error("Could not get model from name");
            throw new IllegalStateException();
        }
        this.encoding = Encodings.newDefaultEncodingRegistry().getEncodingForModel(this.modelType.get());
    }


    /**
     * Checking if the context window of the model is big enough
     *
     * @param chatMessageDtos list of all user and assistant messages
     * @return true if the context window of the model is big enough, false if not
     */
    public boolean isContextLengthValid(List<ChatMessageDto> chatMessageDtos) {
        String contextWindow = systemMessage.getContent() + chatMessageDtos.stream()
                .map(ChatMessageDto::getContent)
                .collect(Collectors.joining());

        int currentContextLength = this.encoding.countTokens(contextWindow);
        if (this.modelType.isPresent()) {
            return (this.modelType.get().getMaxContextLength() > (currentContextLength + maxTokens));
        }
        return false;
    }

    /**
     * Adjusting the number of messages in the context window to fit the model's max context length
     *
     * @param chatMessageDtos list of all user and assistant messages
     * @return same list if model's max context length has not been reached, smaller list if it has
     */
    public List<ChatMessageDto> adjustContextWindow(List<ChatMessageDto> chatMessageDtos) {

        List<ChatMessageDto> chatMessagesDtosAdjusted = new ArrayList<>(chatMessageDtos);

        for (int i = 0; i < chatMessageDtos.size(); i++) {
            if (!isContextLengthValid(chatMessagesDtosAdjusted)) {
                chatMessagesDtosAdjusted.remove(0);
            } else {
                break;
            }
        }
        return chatMessagesDtosAdjusted;
    }
}