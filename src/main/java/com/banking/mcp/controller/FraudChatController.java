package com.banking.mcp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FraudChatController {

    private final ChatClient chatClient;

    public FraudChatController(@Qualifier("mcpChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/fraud/analyze")
    public String analyze(@RequestBody String query) throws JsonProcessingException {
        return chatClient.prompt().user(query).call().content();
    }
}