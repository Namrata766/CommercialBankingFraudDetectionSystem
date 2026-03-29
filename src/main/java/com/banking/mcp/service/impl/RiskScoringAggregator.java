package com.banking.mcp.service.impl;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.*;
import com.banking.mcp.service.port.RiskScoringPort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RiskScoringAggregator implements RiskScoringPort {

    @Override
    public FinalRiskAssessment aggregate(
            PaymentDocument txn,
            AnomalyScore anomaly,
            RuleExecutionResult rules,
            CounterpartyProfile counterparty,
            EwsReputationScore ews,
            BatchPatternAnalysis pattern
    ) {

        double score = 0.0;
        List<String> reasonCodes = new ArrayList<>();

        // 🔷 1. BASE SIGNALS (Weighted)
        double anomalyWeight = 0.3;
        double rulesWeight = 0.3;
        double counterpartyWeight = 0.2;
        double ewsWeight = 0.2;

        double anomalyScore = anomaly.getScore();
        double ruleScore = rules.getAggregateRuleScore();
        double counterpartyScore = mapCounterpartyRisk(counterparty);
        double ewsScore = normalizeEws(ews.getAccountReputationScore());

        score += anomalyScore * anomalyWeight;
        score += ruleScore * rulesWeight;
        score += counterpartyScore * counterpartyWeight;
        score += ewsScore * ewsWeight;

        // 🔷 2. PATTERN INTELLIGENCE (NEW 🔥)
        double patternBoost = 0.0;

        if (pattern != null && pattern.getPatternFlags() != null) {

            if (pattern.getPatternFlags().contains("SMURFING_PATTERN")) {
                patternBoost += 0.2;
                reasonCodes.add("PATTERN_SMURFING");
            }

            if (pattern.getPatternFlags().contains("AMOUNT_PROGRESSION")) {
                patternBoost += 0.1;
                reasonCodes.add("PATTERN_AMOUNT_SEQUENCE");
            }

            if (pattern.getPatternFlags().contains("BURST_ACTIVITY")) {
                patternBoost += 0.1;
                reasonCodes.add("PATTERN_BURST");
            }
        }

        score += patternBoost;

        // 🔷 3. TRANSACTION-LEVEL DIFFERENTIATION (VERY IMPORTANT)
        double amountFactor = normalizeAmount(txn.getAmount(), pattern);
        score += amountFactor * 0.1;

        // 🔷 4. REASON CODES (BASE SIGNALS)
        if (anomalyScore > 0.7) {
            reasonCodes.add("HIGH_ANOMALY");
        } else if (anomalyScore > 0.3) {
            reasonCodes.add("ANOMALY");
        }

        if (ruleScore > 0.3) {
            reasonCodes.add("RULE_TRIGGER");
        }

        if (counterpartyScore > 0.5) {
            reasonCodes.add("COUNTERPARTY_RISK");
        }

        if (ewsScore > 0.7) {
            reasonCodes.add("EWS_HIGH_RISK");
        }

        // 🔷 5. FINAL NORMALIZATION
        score = Math.min(score, 1.0);

        String riskLevel = deriveRiskLevel(score);

        // 🔷 6. BUILD FINAL OBJECT
        FinalRiskAssessment assessment = new FinalRiskAssessment();
        assessment.setPaymentId(txn.getPaymentId());
        assessment.setFinalRiskScore(score);
        assessment.setRiskLevel(riskLevel);
        assessment.setReasonCodes(reasonCodes);
        assessment.setRequiresManualIntervention(score >= 0.6);

        // 🔷 7. ANALYTICAL SUMMARY (FOR LLM 🔥)
        Map<String, Object> summary = new HashMap<>();
        summary.put("anomalyScore", anomalyScore);
        summary.put("ruleScore", ruleScore);
        summary.put("counterpartyScore", counterpartyScore);
        summary.put("ewsScore", ewsScore);
        summary.put("patternFlags", pattern != null ? pattern.getPatternFlags() : List.of());
        summary.put("patternBoost", patternBoost);
        summary.put("amountFactor", amountFactor);
        summary.put("riskDriver", patternBoost > 0 ? "PATTERN_DRIVEN" : "SIGNAL_DRIVEN");

        assessment.setAnalyticalSummary(summary);

        return assessment;
    }

    // 🔷 Helper: Counterparty mapping
    private double mapCounterpartyRisk(CounterpartyProfile profile) {
        if (profile == null || profile.getRelationshipRisk() == null) return 0.0;

        return switch (profile.getRelationshipRisk()) {
            case "HIGH" -> 1.0;
            case "MEDIUM" -> 0.5;
            default -> 0.2;
        };
    }

    // 🔷 Helper: Normalize EWS (0–100 → 0–1)
    private double normalizeEws(int score) {
        return Math.min(score / 100.0, 1.0);
    }

    // 🔷 Helper: Normalize amount relative to batch
    private double normalizeAmount(Double amount, BatchPatternAnalysis pattern) {
        if (pattern == null || pattern.getMaxAmount() == null || pattern.getMaxAmount() == 0) {
            return 0.0;
        }
        return amount / pattern.getMaxAmount();
    }

    // 🔷 Helper: Risk banding
    private String deriveRiskLevel(double score) {
        if (score >= 0.75) return "CRITICAL";
        if (score >= 0.6) return "HIGH";
        if (score >= 0.4) return "MEDIUM";
        return "LOW";
    }
}