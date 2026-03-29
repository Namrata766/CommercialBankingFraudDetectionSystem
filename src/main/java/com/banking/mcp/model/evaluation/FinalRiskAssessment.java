package com.banking.mcp.model.evaluation;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FinalRiskAssessment {
    private String paymentId;
    private double finalRiskScore; // Weighted average of all services
    private String riskLevel;      // e.g., "CRITICAL", "HIGH", "LOW"
    private List<String> reasonCodes;

    // Metadata for the LLM to explain the "Why"
    private Map<String, Object> analyticalSummary;

    // Flag for the UI
    private boolean requiresManualIntervention;
}