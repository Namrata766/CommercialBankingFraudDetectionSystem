package com.banking.mcp.model.evaluation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AnomalyScore {
    private String transactionId;
    private double score; // 0.0 to 1.0
    private List<String> deviations; // e.g., "Amount 300% above mean", "Off-hours"
}
