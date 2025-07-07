package se.lexicon.g54springai.controller;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import se.lexicon.g54springai.service.ChatClientService;
import se.lexicon.g54springai.service.OpenAIService;

import java.io.IOException;

@RestController
@RequestMapping("/api/chat")
public class OpenAIController {

    private final OpenAIService service;
    private final ChatClientService clientService;

    @Autowired
    public OpenAIController(OpenAIService service, ChatClientService clientService) {
        this.service = service;
        this.clientService = clientService;
    }

    @GetMapping
    public String welcome() {
        return "Welcome to the OpenAI Chat API!";
    }

    // http://localhost:8080/api/chat/messages?question=
    @GetMapping("/messages")
    public String processSimpleChatQuery(
            @NotNull(message = "Question cannot be null")
            @NotBlank(message = "Question cannot be blank")
            @Size(max = 200, message = "Question cannot exceed 200 characters")
            @RequestParam String question
    ) {
        return service.processSimpleChatQuery(question);
    }


    @GetMapping("/messages/stream")
    public Flux<String> processSimpleChatQueryWithStream(
            @NotNull(message = "Question cannot be null")
            @NotBlank(message = "Question cannot be blank")
            @Size(max = 200, message = "Question cannot exceed 200 characters")
            @RequestParam String question
    ) {
        return service.processSimpleChatQueryWithStream(question);
    }


    @GetMapping("/messages/lexbot")
    public String processSimpleChatQueryWithContext(
            @NotNull(message = "Question cannot be null")
            @NotBlank(message = "Question cannot be blank")
            @Size(max = 200, message = "Question cannot exceed 200 characters")
            @RequestParam String question
    ) {
        return service.processSimpleChatQueryWithContext(question);
    }


    @PostMapping("/images/describe")
    public String AskToProcessImage(@RequestParam
                                    @NotNull(message = "File cannot be null")
                                    MultipartFile file) {
        return service.processImage(file);

    }

    @GetMapping("/images/generate/url")
    public String generateImageAndReturnUrl(
            @NotNull(message = "Query cannot be null")
            @NotBlank(message = "Query cannot be blank")
            @Size(max = 1000, message = "Query cannot exceed 1000 characters")
            @RequestParam String query) {
        return service.generateImageAndReturnUrl(query);
    }


    @PostMapping("/speech-to-text")
    public String speechToText(@RequestParam("file") MultipartFile file) throws IOException {
        return service.speechToText(file);
    }

    @GetMapping("/text-to-speech")
    public ResponseEntity<byte[]> streamAudio(@RequestParam String text) throws IOException {
        byte[] audioData = service.textToSpeech(text);

        // Set headers to indicate streaming audio
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
        //headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=output.mp3");
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=speech_" + System.currentTimeMillis() + ".mp3");


        return new ResponseEntity<>(audioData, headers, HttpStatus.OK);

    }


    @GetMapping("/messages/chat-memory")
    public String chatMemory(
            @RequestParam
            @NotNull(message = "Conversation ID cannot be null")
            @NotBlank(message = "Conversation ID cannot be blank")
            @Size(max = 36, message = "Conversation ID cannot exceed 36 characters")
            String conversationId,
            @RequestParam
            @NotNull(message = "Question cannot be null")
            @NotBlank(message = "Question cannot be blank")
            @Size(max = 200, message = "Question cannot exceed 200 characters")
            String question) {
        System.out.println("conversationId = " + conversationId);
        System.out.println("question = " + question);
        return service.chatMemory(question, conversationId);
    }

    @GetMapping("/reset-chat")
    public void resetChat(
            @RequestParam
            @NotNull(message = "Conversation ID cannot be null")
            @NotBlank(message = "Conversation ID cannot be blank")
            @Size(max = 36, message = "Conversation ID cannot exceed 36 characters")
            String conversationId) {
        service.resetChatMemory(conversationId);
    }


    @GetMapping("/messages/new-chat-memory")
    public String newChatMemory(@RequestParam
                                @NotNull(message = "Conversation ID cannot be null")
                                @NotBlank(message = "Conversation ID cannot be blank")
                                @Size(max = 36, message = "Conversation ID cannot exceed 36 characters")
                                String conversationId,
                                @RequestParam
                                @NotNull(message = "Question cannot be null")
                                @NotBlank(message = "Question cannot be blank")
                                @Size(max = 200, message = "Question cannot exceed 200 characters")
                                String question) {
        System.out.println("conversationId = " + conversationId);
        System.out.println("question = " + question);
        return clientService.chatMemory(question, conversationId);
    }

}