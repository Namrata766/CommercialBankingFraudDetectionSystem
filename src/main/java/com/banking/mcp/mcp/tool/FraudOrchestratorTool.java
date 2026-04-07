package com.banking.mcp.mcp.tool;

import com.banking.mcp.orchestration.FraudEvaluationOrchestrator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FraudOrchestratorTool implements ToolCallback {

    private final FraudEvaluationOrchestrator orchestrator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FraudOrchestratorTool(FraudEvaluationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
        System.err.println("FraudOrchestratorTool created");
    }

    @Override
    public String call(String args) {
        try {
            Map<String, Object> arguments = objectMapper.readValue(args, Map.class);
            String query = (String) arguments.get("query");
            if (query != null) {
                return orchestrator.evaluateStructured(query);
            } else {
                return "Invalid arguments: query not found";
            }
        } catch (JsonProcessingException e) {
            return "Error parsing arguments: " + e.getMessage();
        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("fraud_evaluation_orchestrator")
            .description("Evaluates transactions for fraud risk using multiple signals")
            .inputSchema("{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"The query describing the transactions to evaluate for fraud\"}}}")
            .build();
    }
}
