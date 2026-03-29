package com.banking.mcp.service.impl;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.RuleExecutionResult;
import com.banking.mcp.model.evaluation.TriggeredRule;
import com.banking.mcp.service.port.RulesEnginePort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RulesEngineService implements RulesEnginePort {

    @Override
    public RuleExecutionResult evaluate(PaymentDocument payment) {

        RuleExecutionResult result = new RuleExecutionResult();

        result.setTriggeredRules(List.of(
                new TriggeredRule("RULE_001", "High value transaction", 0.5)
        ));

        result.setAggregateRuleScore(0.5);

        return result;
    }
}