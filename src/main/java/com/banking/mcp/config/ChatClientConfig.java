package com.banking.mcp.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        // Create a builder without tool calling to avoid circular dependency
        return ChatClient.builder(chatModel);
    }

    @Bean
    public ChatClient toolChatClient(ChatClient.Builder chatClientBuilder) {
        // This is the tool-unaware chat client for internal use
        return chatClientBuilder.build();
    }
}