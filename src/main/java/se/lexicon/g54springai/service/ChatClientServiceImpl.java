package se.lexicon.g54springai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatClientServiceImpl implements ChatClientService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final AppToolCalling appToolCalling;

    @Autowired
    public ChatClientServiceImpl(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, AppToolCalling appToolCalling) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.chatMemory = chatMemory;
        this.appToolCalling = appToolCalling;
    }

    @Override
    public String chatMemory(String question, String conversationId) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be null or empty");
        }
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty");
        }

        ChatResponse chatResponse = this.chatClient.prompt()
                .user(question)
                .system("""
                        You are a specialized name management assistant with the following capabilities:
                        1. You can fetch and display all stored names using the 'fetAllNames' tool
                        2. You can search for specific names using the 'findByName' tool
                        3. You can add new names using the 'addNewName' tool
                        
                        Guidelines:
                        - Always use the appropriate tool for name-related operations
                        - Only respond with name-related information
                        - If a request is not about names, politely explain that you can only help with name management
                        - When displaying names, present them in a clear, organized manner
                        - Confirm successful operations with brief, clear messages
                        """)
                .tools(appToolCalling) // this class contains 3 methods that AI can call
                .options(OpenAiChatOptions.builder().temperature(0.2).maxTokens(1000).build())
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .chatResponse();

        Generation result = null;
        if (chatResponse != null) {
            result = chatResponse.getResult();
        }
        return result != null ? result.getOutput().getText() : "No response received";
    }
}