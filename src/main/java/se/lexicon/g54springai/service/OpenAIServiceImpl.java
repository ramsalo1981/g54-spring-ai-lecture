package se.lexicon.g54springai.service;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;


@Service
public class OpenAIServiceImpl implements OpenAIService {

    private final OpenAiChatModel openAiChatModel;
    // It represents a chat model from OpenAI that can be used to process chat queries.
    // It supports both synchronous and asynchronous operations.
    // .call() method is used to send a chat message and receive a response.
    // .stream() method is used to stream responses in real-time.

    private final OpenAiImageModel openAiImageModel;

    // OpenAiAudioTranscriptionModel is used for audio transcription tasks
    // OpenAiAudioSpeechModel is used for speech synthesis tasks
    private OpenAiAudioTranscriptionModel openaiAudioTranscriptionModel;
    private OpenAiAudioSpeechModel openaiAudioSpeechModel;


    @Autowired
    public OpenAIServiceImpl(OpenAiChatModel openAiChatModel, OpenAiImageModel openAiImageModel,
                             OpenAiAudioTranscriptionModel openaiAudioTranscriptionModel,
                             OpenAiAudioSpeechModel openaiAudioSpeechModel) {
        this.openAiChatModel = openAiChatModel;
        this.openAiImageModel = openAiImageModel;
        this.openaiAudioTranscriptionModel = openaiAudioTranscriptionModel;
        this.openaiAudioSpeechModel = openaiAudioSpeechModel;
    }

    @Override
    public String processSimpleChatQuery(final String query) {

        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        try {
            return openAiChatModel.call(query);
        } catch (RuntimeException e) {
            // Handle the exception, log it, or rethrow it as needed
            throw new RuntimeException("Error processing chat query: " + e.getMessage(), e);
        }

    }

    @Override
    public Flux<String> processSimpleChatQueryWithStream(final String query) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        try {
            return openAiChatModel.stream(query);
        } catch (RuntimeException e) {
            // Handle the exception, log it, or rethrow it as needed
            throw new RuntimeException("Error processing chat query: " + e.getMessage(), e);
        }
    }

    @Override
    public String processSimpleChatQueryWithContext(String query) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        // system message sets the context(behavior/personality/instruction/tone) for the AI models
        SystemMessage systemMessage = SystemMessage.builder().text("You are ans AI Assistant named LEXBOT").build();
        // user message is the actual query from the user
        UserMessage userMessage = UserMessage.builder().text(query).build();
        // prompt represents the entire chat interaction, including system and user messages (full input to llm)
        Prompt prompt = Prompt.builder()
                .messages(systemMessage, userMessage)
                .chatOptions(
                        ChatOptions.builder()
                                .model("gpt-4.1-mini")
                                .temperature(0.3)
                                .build()
                )
                .build();
        ChatResponse chatResponse = openAiChatModel.call(prompt);
        return chatResponse.getResult() != null ? chatResponse.getResult().getOutput().getText() : "No response generated";
    }

    @Override
    public String processImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Invalid file type. File must be an image");
        }

        Media media = Media.builder()
                .data(file.getResource())
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .build();
        UserMessage userMessage = UserMessage.builder()
                .text("Explain what do you see on this picture?")
                .media(media)
                .build();
        SystemMessage systemMessage = SystemMessage.builder()
                .text("You are a helpful assistant that describes the contents of an image.")
                .build();
        Prompt prompt = Prompt.builder()
                .messages(systemMessage, userMessage)
                .chatOptions(
                        ChatOptions.builder()
                                .model("gpt-4.1-mini")
                                .temperature(0.3)
                                .build()
                )
                .build();
        ChatResponse chatResponse = openAiChatModel.call(prompt);
        return chatResponse.getResult() != null ? chatResponse.getResult().getOutput().getText() : "No description generated";
    }

    @Override
    public String generateImageAndReturnUrl(String query) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        System.out.println("query = " + query);
        String systemInstructionTemplate = String.format("""
                    Create a highly detailed, professional image following these specifications:
                    Subject: %s
                    Technical Guidelines:
                    - Avoid text or writing in the image
                    - Ensure family-friendly content
                    - Focus on clear, sharp details
                    - Use balanced color composition
                """, query);

        ImageOptions imageOptions = OpenAiImageOptions.builder()
                .model("dall-e-3") // Using DALL-E 3 for better quality
                .quality("hd")      // Set image quality to high definition
                .N(1)               // Generate 1 image
                .responseFormat("url")  // Get URL
                .build();

        ImagePrompt imagePrompt = new ImagePrompt(systemInstructionTemplate, imageOptions);
        ImageResponse imageResponse = openAiImageModel.call(imagePrompt);
        List<ImageGeneration> images = imageResponse.getResults();
        ImageGeneration firstImage = images.get(0);

        String strURL = firstImage.getOutput().getUrl();

        // download and save the image to the local file system
        try (InputStream in = URI.create(strURL).toURL().openStream()) {
            Files.copy(in, Paths.get("generated_image_" + System.currentTimeMillis() + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return strURL;

    }


    public String speechToText(MultipartFile file) {
        try {
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile("audio_", "_upload");
                // createTempFile() method creates a temporary file in the default temporary-file directory.
                file.transferTo(tempFile.toFile());
                // transferTo() method copies the contents of the MultipartFile to the specified file.

                // Configure audio transcription options
                OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions
                        .builder()
                        .language("en")
                        .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.VTT)
                        .build();

                AudioTranscriptionPrompt transcriptionPrompt = new AudioTranscriptionPrompt(
                        new FileSystemResource(tempFile.toFile()),
                        options);

                return openaiAudioTranscriptionModel.call(transcriptionPrompt).getResult().getOutput();
            } finally {
                // Clean up the temporary file
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error processing audio file: " + e.getMessage(), e);
        }
    }

    public byte[] textToSpeech(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Text cannot be null or empty");
            }
            byte[] audioData = openaiAudioSpeechModel.call(text);
            // Configure speech options
            OpenAiAudioApi.SpeechRequest request = OpenAiAudioApi.SpeechRequest.builder()
                    .input(text)
                    .model("tts-1") // or "tts-1-hd" for higher quality
                    .voice("alloy") // Available voices: alloy, echo, fable, onyx, nova, shimmer
                    .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3) // Response format can be MP3 or WAV
                    .speed(1.0f) // Speech speed (0.25 to 4.0)
                    .build();// Get the audio data

            // Save to local file
            String fileName = "generated_speech_" + System.currentTimeMillis() + ".mp3";
            Path filePath = Paths.get(fileName);
            Files.write(filePath, audioData);
            System.out.println("Audio saved to: " + filePath.toAbsolutePath());

            return audioData;
        } catch (IOException e) {
            throw new RuntimeException("Error generating speech: " + e.getMessage(), e);
        }
    }
}
