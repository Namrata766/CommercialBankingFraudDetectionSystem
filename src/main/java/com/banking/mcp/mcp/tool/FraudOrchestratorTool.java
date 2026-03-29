package com.banking.mcp.mcp.tool;

import com.banking.mcp.mcp.dto.FraudQueryRequest;
import com.banking.mcp.mcp.dto.FraudQueryResponse;
import com.banking.mcp.orchestration.FraudEvaluationOrchestrator;
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
    public FraudQueryResponse evaluate(FraudQueryRequest request) {
        return orchestrator.execute(request);
    }
}