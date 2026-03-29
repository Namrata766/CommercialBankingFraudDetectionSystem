package com.banking.mcp.model.evaluation;

import lombok.Data;

import java.util.List;

@Data
public class RuleExecutionResult {
    private List<TriggeredRule> triggeredRules;
    private double aggregateRuleScore;
}
