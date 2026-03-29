package com.banking.mcp.service.impl;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.*;
import com.banking.mcp.service.port.RiskScoringPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RiskScoringAggregator implements RiskScoringPort {

    @Override
    public FinalRiskAssessment aggregate(
            PaymentDocument txn,
            AnomalyScore anomaly,
            RuleExecutionResult rules,
            CounterpartyProfile counterparty,
            EwsReputationScore ews
    ) {

        double finalScore =
                anomaly.getScore() * 0.4 +
                rules.getAggregateRuleScore() * 0.3 +
                (counterparty.isFirstTimeInteraction() ? 0.2 : 0.1) +
                (ews.isReportedFraudulent() ? 0.5 : 0.0);

        String riskLevel = mapRisk(finalScore);

        FinalRiskAssessment result = new FinalRiskAssessment();
        result.setPaymentId(txn.getPaymentId());
        result.setFinalRiskScore(finalScore);
        result.setRiskLevel(riskLevel);
        result.setReasonCodes(List.of("ANOMALY", "RULE_TRIGGER"));
        result.setRequiresManualIntervention(finalScore > 0.7);

        result.setAnalyticalSummary(Map.of(
                "amount", txn.getAmount(),
                "anomalyScore", anomaly.getScore(),
                "ruleScore", rules.getAggregateRuleScore(),
                "ewsScore", ews.getAccountReputationScore()
        ));

        return result;
    }

    private String mapRisk(double score) {
        if (score > 0.8) return "CRITICAL";
        if (score > 0.6) return "HIGH";
        if (score > 0.3) return "MEDIUM";
        return "LOW";
    }
}