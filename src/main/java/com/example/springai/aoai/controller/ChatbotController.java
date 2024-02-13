package com.example.springai.aoai.controller;

import com.example.springai.aoai.dto.ChatMessageDto;
import com.example.springai.aoai.service.ChatbotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.Generation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Constructor
     *
     * @param chatbotService the chatbotService
     */
    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * Requesting completion from chatbot
     *
     * @param chatMessagesDtos list of all messages (user and assistant) stored on client side, as server side is stateless
     * @return the last answer of the chatbot
     */
    @PostMapping("/completion")
    public ResponseEntity<String> completion(@RequestBody List<ChatMessageDto> chatMessagesDtos) {

        log.info("Calling /completion API");

        if (chatMessagesDtos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Body should contain at least 1 message.");
        }

        //If context window's length is too big, need to delete old messages
        chatMessagesDtos = chatbotService.adjustContextWindow(chatMessagesDtos);
        if (chatMessagesDtos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Question's length is too big.");
        }

        Optional<Generation> gen = chatbotService.completion(chatMessagesDtos);
        if (gen.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(gen.get().getOutput().getContent());
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal server error occurred.");
        }
    }

    /**
     * Requesting completion from chatbot in stream mode
     *
     * @param chatMessagesDtos list of all messages (user and assistant) stored on client side, as server side is stateless
     * @return a Flux containing the answer
     */
    @PostMapping("/completion/stream")
    public Flux<String> completionWithStream(@RequestBody List<ChatMessageDto> chatMessagesDtos) {

        log.info("Calling /completion/stream API");

        if (chatMessagesDtos.isEmpty()) {
            return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body should contain at least 1 message."));
        }

        //If context window's length is too big, need to delete old messages
        chatMessagesDtos = chatbotService.adjustContextWindow(chatMessagesDtos);
        if (chatMessagesDtos.isEmpty()) {
            return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question's length is too big."));
        }

        Flux<Generation> generation = chatbotService.completionWithStream(chatMessagesDtos);
        return generation.filter(i -> i.getOutput().getContent() != null).map(s -> s.getOutput().getContent());
    }
}