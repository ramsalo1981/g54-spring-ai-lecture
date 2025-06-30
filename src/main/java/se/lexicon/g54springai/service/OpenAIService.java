package se.lexicon.g54springai.service;

import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

public interface OpenAIService {

    String processSimpleChatQuery(String query);

    Flux<String> processSimpleChatQueryWithStream(String query);

    String processSimpleChatQueryWithContext(String query);

    String processImage(MultipartFile file);

    String generateImageAndReturnUrl(String query);

    String speechToText(MultipartFile file);

    byte[] textToSpeech(String text);

}
