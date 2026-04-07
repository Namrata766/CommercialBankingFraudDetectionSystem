package com.banking.mcp.mcp.tool;

import com.banking.mcp.orchestration.FraudEvaluationOrchestrator;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class FraudOrchestratorTool {

    private final FraudEvaluationOrchestrator orchestrator;

    public FraudOrchestratorTool(FraudEvaluationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Tool(
            name = "fraud_evaluation_orchestrator",
            description = "Evaluates transactions for fraud risk using multiple signals"
    )
    public String evaluate(String query) throws JsonProcessingException {
        return orchestrator.evaluateStructured(query);
    }
}