package com.banking.mcp.service.impl;

import com.banking.mcp.mcp.dto.FraudQueryResponse;
import com.banking.mcp.util.FraudReasonTranslator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class LlmExplanationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatClient chatClient;

    public LlmExplanationService(@Qualifier("toolChatClient") @Lazy ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String generateExplanation(FraudQueryResponse response) throws JsonProcessingException {

        // 🔷 Enrich response with natural language reasons and risk levels
        enrichResponseWithNaturalLanguageReasons(response);

        log.info("Fraud Query Response (Enriched): {}", objectMapper.writeValueAsString(response));
        String prompt = buildPrompt(response);
        log.info("Generated LLM Prompt: {}", prompt);

        log.info("Calling OpenAI LLM for explanation...");
        String result = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        log.info("LLM response received, length: {}", result.length());

        return result;
    }

    /**
     * Enrich each FraudResult with natural language reasons, risk levels, and next steps
     */
    private void enrichResponseWithNaturalLanguageReasons(FraudQueryResponse response) {
        if (response.getResults() == null) {
            return;
        }

        for (FraudQueryResponse.FraudResult result : response.getResults()) {
            // 🔷 Translate technical signals to natural language
            List<FraudReasonTranslator.ReasonWithSource> reasons =
                    FraudReasonTranslator.translateReasons(result);

            List<String> formattedReasons = reasons.stream()
                    .map(FraudReasonTranslator.ReasonWithSource::formatted)
                    .toList();

            // 🔷 Ensure highlights map exists
            if (result.getHighlights() == null) {
                result.setHighlights(new HashMap<>());
            }

            // 🔷 Add enriched data
            result.getHighlights().put("naturalLanguageReasons", formattedReasons);
            result.getHighlights().put("riskLevelLabel", FraudReasonTranslator.deriveRiskLevelLabel(result.getRiskScore()));
            result.getHighlights().put("nextSteps", FraudReasonTranslator.getNextSteps(result));
        }
    }

    private String buildPrompt(FraudQueryResponse response) throws JsonProcessingException {

        return """
                You are a banking fraud analyst preparing a structured report for Claude Desktop.
                
                Your output must be clear, concise, and human-readable. Use the data EXACTLY as provided.
                
                STRICT INSTRUCTIONS:
                ✓ DO use provided natural language reasons and risk levels
                ✓ DO format each transaction with: ID, Amount, From, To, Rail, Risk Level, Why Flagged, Recommended Review
                ✓ DO copy naturalLanguageReasons and nextSteps verbatim from the data
                ✓ DO NOT include scores, percentages, or technical jargon
                ✓ DO NOT repeat information across sections
                ✓ DO NOT suggest regulatory actions (SARs, holds, freezes) — these are for human review
                ✓ DO NOT add sections beyond the template below
                
                FORMAT YOUR RESPONSE EXACTLY AS FOLLOWS:
                
                === PORTFOLIO SUMMARY ===
                Total Transactions Analyzed: [N]
                Overall Risk Level: [HIGH | MEDIUM | LOW]
                Transactions Flagged: [X at HIGH, Y at MEDIUM, Z at LOW]
                Cross-Transaction Patterns: [Yes/No]
                
                === TRANSACTION DETAILS ===
                [For each transaction, ordered by risk level (HIGH first)]
                
                **Transaction ID:** [paymentId]
                **Amount:** $[amount] [currency]
                **From:** [debtorAccountMasked]
                **To:** [creditorAccountMasked]
                **Rail:** [rail]
                **Risk Level:** [Use riskLevelLabel from highlights]
                
                **Why Flagged:**
                [Copy naturalLanguageReasons list verbatim from highlights]
                
                **Recommended Review:**
                [Copy nextSteps verbatim from highlights]
                
                ---
                
                === KEY INSIGHTS ===
                [2-3 bullet points with the most important cross-transaction observations. Do NOT repeat transaction details.]
                
                DATA:
                %s
                """.formatted(objectMapper.writeValueAsString(response));
    }
}