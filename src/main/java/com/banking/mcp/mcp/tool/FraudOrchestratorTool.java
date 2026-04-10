package com.banking.mcp.mcp.tool;

import com.banking.mcp.orchestration.FraudEvaluationOrchestrator;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FraudOrchestratorTool {

    private final FraudEvaluationOrchestrator orchestrator;

    public FraudOrchestratorTool(FraudEvaluationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
        System.err.println("FraudOrchestratorTool created with @McpTool");
    }

    @McpTool(
            name = "fraud_evaluation_orchestrator",
            description = "Evaluates transactions for fraud risk using multiple signals"
    )
    public String evaluateFraud(Map<String, Object> args) {
        try {
            String query = (String) args.get("query");
            if (query != null) {
                return orchestrator.evaluateStructured(query);
            } else {
                return "Invalid arguments: query not found";
            }
        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }
}