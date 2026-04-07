package com.banking.mcp.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean("toolChatClient")
    public ChatClient toolChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean("mcpChatClient")
    public ChatClient mcpChatClient(ChatClient.Builder builder, ToolCallbackProvider toolCallbackProvider) {
        return builder.defaultToolCallbacks(toolCallbackProvider).build();
    }
}