package com.banking.mcp.service.port;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.*;

public interface RiskScoringPort {

    FinalRiskAssessment aggregate(
            PaymentDocument txn,
            AnomalyScore anomaly,
            RuleExecutionResult rules,
            CounterpartyProfile counterparty,
            EwsReputationScore ews,
            BatchPatternAnalysis pattern
    );
}