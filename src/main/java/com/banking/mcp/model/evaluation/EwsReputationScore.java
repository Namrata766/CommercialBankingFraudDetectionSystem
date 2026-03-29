package com.banking.mcp.model.evaluation;

import lombok.Data;

@Data
public class EwsReputationScore {
    private int accountReputationScore; // 0-100
    private boolean isReportedFraudulent;
    private String source;
}