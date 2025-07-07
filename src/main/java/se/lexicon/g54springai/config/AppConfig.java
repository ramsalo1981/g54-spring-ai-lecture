package se.lexicon.g54springai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("se.lexicon.*")
public class AppConfig {

    @Bean
    public ChatMemory chatMemory() {
        // Chat Memory Types:
        // 1. MessageWindowChatMemory: Keeps a fixed number of recent messages.
        // 2. ChatMemoryRepository: Stores messages in a database or persistent storage.
        // 3. VectorStoreChatMemory: Uses vector embeddings for advanced retrieval.
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build(); // Default window size is 20 messages.
    }
}