package com.banking.mcp.controller;

import com.banking.mcp.orchestration.FraudEvaluationOrchestrator;
import com.banking.mcp.service.impl.LlmExplanationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FraudChatController {

    private final ChatClient chatClient;
    private final FraudEvaluationOrchestrator orchestrator;
    private final LlmExplanationService explanationService;

    public FraudChatController(@Qualifier("mcpChatClient") ChatClient chatClient, FraudEvaluationOrchestrator orchestrator, LlmExplanationService explanationService) {
        this.chatClient = chatClient;
        this.orchestrator = orchestrator;
        this.explanationService = explanationService;
    }

    @PostMapping("/fraud/analyze")
    public String analyze(@RequestBody String query) throws JsonProcessingException {
        return chatClient.prompt().user(query).call().content();
    }
}