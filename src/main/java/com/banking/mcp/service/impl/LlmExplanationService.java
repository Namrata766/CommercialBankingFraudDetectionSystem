package com.banking.mcp.service.impl;

import com.banking.mcp.mcp.dto.FraudQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmExplanationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatClient chatClient;

    public String generateExplanation(FraudQueryResponse response) throws JsonProcessingException {

        String prompt = buildPrompt(response);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String buildPrompt(FraudQueryResponse response) throws JsonProcessingException {

        return """
                You are a banking fraud analyst.

                Analyze the following fraud detection results and provide:
                1. A concise summary of risk
                2. Highlight top risky transactions
                3. Mention key reasons for risk

                Data:
                %s
                """.formatted(objectMapper.writeValueAsString(response));
    }
}