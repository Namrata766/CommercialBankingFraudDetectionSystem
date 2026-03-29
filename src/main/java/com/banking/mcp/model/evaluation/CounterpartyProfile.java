package com.banking.mcp.model.evaluation;

import lombok.Data;

@Data
public class CounterpartyProfile {
    private boolean isFirstTimeInteraction;
    private String relationshipRisk; // LOW, MEDIUM, HIGH
    private String countryRiskLevel;
}