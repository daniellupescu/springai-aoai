package com.example.springai.aoai;

import com.example.springai.aoai.dto.ChatMessageDto;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Class containing methods that will be executed after the application started.
 */
@Component
public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {


    /**
     * Calls the /chat/completion endpoint from Chatbot controller
     *
     * @param chatMessagesDtos list of ChatMessageDto
     * @param baseUrl          base url
     */
    private static void callChatCompletionAPI(List<ChatMessageDto> chatMessagesDtos, String baseUrl) {
        RestClient restClient = RestClient.create(baseUrl);
        String completion = restClient.post().uri("/chat/completion")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(chatMessagesDtos)
                .retrieve()
                .body(String.class);
        System.out.println(completion);
    }

    /**
     * Calls the /chat/completion/stream endpoint from Chatbot controller
     *
     * @param chatMessagesDtos list of ChatMessageDto
     * @param baseUrl          base url
     */
    private static void callChatCompletionStreamAPI(List<ChatMessageDto> chatMessagesDtos, String baseUrl) {
        WebClient client = WebClient.create(baseUrl);
        Flux<String> flux = client.post()
                .uri("/chat/completion/stream")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(chatMessagesDtos), ChatMessageDto.class)
                .retrieve()
                .bodyToFlux(String.class);
        flux.subscribe(System.out::println);
    }

    /**
     * Runs basic tests on startup
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        //Initializing test data
        List<ChatMessageDto> chatMessagesDtos = new ArrayList<>();
        chatMessagesDtos.add(new ChatMessageDto("user", "Explain how to build an Operating System"));
        String baseUrl = "http://localhost:8080";

        //Calling "normal" and "stream" chat completion APIs
        callChatCompletionAPI(chatMessagesDtos, baseUrl);
        callChatCompletionStreamAPI(chatMessagesDtos, baseUrl);
    }

}
