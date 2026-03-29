package com.banking.mcp.controller;

import com.banking.mcp.mcp.dto.FraudQueryResponse;
import com.banking.mcp.orchestration.FraudEvaluationOrchestrator;
import com.banking.mcp.service.impl.LlmExplanationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FraudChatController {

    private final ChatClient chatClient;
    private final FraudEvaluationOrchestrator orchestrator;
    private final LlmExplanationService explanationService;

    @PostMapping("/fraud/analyze")
    public String analyze(@RequestBody String query) throws JsonProcessingException {

        // Step 1: Get structured result
        FraudQueryResponse response =
                orchestrator.evaluateStructured(query);

        // Step 2: Generate explanation
        String explanation =
                explanationService.generateExplanation(response);

        return explanation;
    }
}