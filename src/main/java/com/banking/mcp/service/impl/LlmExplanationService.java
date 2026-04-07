package com.banking.mcp.service.impl;

import com.banking.mcp.mcp.dto.FraudQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LlmExplanationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatClient chatClient;

    public LlmExplanationService(@Qualifier("toolChatClient") @Lazy ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String generateExplanation(FraudQueryResponse response) throws JsonProcessingException {

        log.info("Fraud Query Response: {}", objectMapper.writeValueAsString(response));
        String prompt = buildPrompt(response);
        log.info("Generated LLM Prompt: {}", prompt);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String buildPrompt(FraudQueryResponse response) throws JsonProcessingException {

        return """
                You are a banking fraud analyst.
                
                Analyze ONLY the fraud detection results provided below.
                These are actual evaluated transactions.
                
                Do NOT say you lack data.
                Do NOT ask for additional data.
                Do NOT provide generic fraud frameworks.
                
                Provide:
                
                1) Concise summary of risk
                - Overall risk level
                - Whether risk is driven by individual transactions or patterns
                
                2) Top risky transactions (ranked by riskScore)
                - Include transaction ID, amount, parties (masked), and risk score
                
                3) Key reasons for elevated risk
                Focus on:
                - Patterns across transactions
                - Smurfing / structuring behavior
                - Repeated or linked accounts
                - Rule triggers and EWS signals
                - Any notable behavioral patterns
                
                Important:
                - Use ONLY the provided data
                - Do NOT generate synthetic examples
                - Do NOT return JSON
                - Keep the response concise and readable
                - Do NOT add extra sections
                
                STRICT BEHAVIOR RULES:
                
                - Treat the provided data as COMPLETE for analysis
                - Do NOT mention missing data
                - Do NOT say data is insufficient
                - Do NOT ask for more data
                - Do NOT suggest re-running analysis
                - Do NOT provide next steps or options
                - Do NOT act as a consultant
                
                Your job is ONLY to analyze and summarize the given transactions.
                
                DATA:
                %s
                """.formatted(objectMapper.writeValueAsString(response));
    }
}