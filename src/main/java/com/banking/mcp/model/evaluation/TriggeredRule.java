package com.banking.mcp.model.evaluation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TriggeredRule {
    private String ruleId;
    private String description;
    private double weight;
}