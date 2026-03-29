package com.banking.mcp.service.port;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.RuleExecutionResult;

public interface RulesEnginePort {

    RuleExecutionResult evaluate(PaymentDocument payment);
}