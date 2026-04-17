package com.banking.mcp.util;

import com.banking.mcp.mcp.dto.FraudQueryResponse;
import java.util.*;

/**
 * Translates technical fraud signals into human-readable natural language reasons.
 * Maps rule triggers, EWS scores, and anomaly signals to business-friendly explanations.
 */
public class FraudReasonTranslator {

    /**
     * Convert a FraudResult into a list of natural language reasons with their sources
     */
    public static List<ReasonWithSource> translateReasons(FraudQueryResponse.FraudResult result) {
        List<ReasonWithSource> reasons = new ArrayList<>();

        Map<String, Object> summary = result.getAnalyticalSummary();
        if (summary == null) {
            return reasons;
        }

        double anomalyScore = getDoubleValue(summary, "anomalyScore");
        double ruleScore = getDoubleValue(summary, "ruleScore");
        double ewsScore = getDoubleValue(summary, "ewsScore");
        double counterpartyScore = getDoubleValue(summary, "counterpartyScore");
        double amountFactor = getDoubleValue(summary, "amountFactor");
        @SuppressWarnings("unchecked")
        List<String> patternFlags = (List<String>) summary.getOrDefault("patternFlags", List.of());

        // 🔷 ANOMALY-DRIVEN REASONS
        if (anomalyScore > 0.7) {
            reasons.add(new ReasonWithSource(
                    "Significant statistical deviation from account's historical behavior",
                    "ANOMALY"
            ));
        } else if (anomalyScore > 0.3) {
            reasons.add(new ReasonWithSource(
                    "Moderate deviation from typical transaction patterns",
                    "ANOMALY"
            ));
        }

        // 🔷 RULE-DRIVEN REASONS
        if (ruleScore > 0.5) {
            addRuleTriggerReasons(reasons, result, ruleScore);
        }

        // 🔷 EWS-DRIVEN REASONS
        if (ewsScore > 0.7) {
            reasons.add(new ReasonWithSource(
                    "External watchlist or reputation service flagged counterparty",
                    "EWS"
            ));
        } else if (ewsScore > 0.3) {
            reasons.add(new ReasonWithSource(
                    "Counterparty has elevated external risk indicators",
                    "EWS"
            ));
        }

        // 🔷 COUNTERPARTY-DRIVEN REASONS
        if (counterpartyScore > 0.5) {
            reasons.add(new ReasonWithSource(
                    "Counterparty has history of risky relationships or unusual activity",
                    "COUNTERPARTY"
            ));
        }

        // 🔷 AMOUNT-DRIVEN REASONS
        if (result.getAmount() >= 100_000 && amountFactor > 0.5) {
            reasons.add(new ReasonWithSource(
                    "High-value transaction exceeds rail-specific thresholds",
                    "RULE_001"
            ));
        }

        // 🔷 PATTERN-DRIVEN REASONS
        if (!patternFlags.isEmpty()) {
            for (String flag : patternFlags) {
                reasons.add(new ReasonWithSource(
                        translatePatternFlag(flag),
                        "PATTERN"
                ));
            }
        }

        // 🔷 SELF-TRANSFER REASONS
        if (result.getDebtorAccountMasked().equals(result.getCreditorAccountMasked())) {
            reasons.add(new ReasonWithSource(
                    "Self-transfer (same payer and payee)",
                    "STRUCTURAL"
            ));
        }

        return reasons.isEmpty() ? List.of(
                new ReasonWithSource("Transaction triggered fraud detection rules", "SYSTEM")
        ) : reasons;
    }

    /**
     * Add rule-specific reasons based on the payment details
     */
    private static void addRuleTriggerReasons(List<ReasonWithSource> reasons, FraudQueryResponse.FraudResult result, @SuppressWarnings("unused") double ruleScore) {
        double amount = result.getAmount();
        String rail = result.getRail();

        // Check for amount thresholds
        if ("WIRE".equalsIgnoreCase(rail) && amount >= 100_000) {
            reasons.add(new ReasonWithSource(
                    "High-value wire transfer ($" + String.format("%.0f", amount) + ") above threshold",
                    "RULE_001"
            ));
        } else if ("ACH".equalsIgnoreCase(rail) && amount >= 50_000) {
            reasons.add(new ReasonWithSource(
                    "High-value ACH transfer ($" + String.format("%.0f", amount) + ") above threshold",
                    "RULE_001"
            ));
        } else if ("INSTANT".equalsIgnoreCase(rail) && amount >= 25_000) {
            reasons.add(new ReasonWithSource(
                    "High-value instant payment ($" + String.format("%.0f", amount) + ") above threshold",
                    "RULE_001"
            ));
        }

        // Check for rejection patterns (account probing)
        if (amount < 10 && amount > 0) {
            reasons.add(new ReasonWithSource(
                    "Micro-transaction pattern consistent with account probing",
                    "RULE_004"
            ));
        }
    }

    /**
     * Convert pattern flags to natural language
     */
    private static String translatePatternFlag(String flag) {
        return switch (flag) {
            case "SMURFING_PATTERN" -> "Multiple small transactions in short period (potential structuring)";
            case "SAME_BENEFICIARY" -> "Repeated transfers to same payee";
            case "AMOUNT_PROGRESSION" -> "Transaction amounts show monotonic progression";
            case "BURST_ACTIVITY" -> "Multiple transactions clustered within short time window";
            case "OFFSHORE_DESTINATION" -> "Payment routed to high-risk jurisdiction";
            case "UNUSUAL_HOURS" -> "Transaction executed outside normal business hours";
            case "ROUND_AMOUNTS" -> "Transaction uses round dollar amounts (e.g., $1000, $5000)";
            case "SAME_DEBTOR_MULTIPLE_CREDITORS" -> "Single payer distributing to multiple payees";
            case "HIGH_FREQUENCY" -> "High-frequency activity detected (5+ transactions per hour)";
            case "CHANNEL_ANOMALY" -> "Mobile channel used during off-hours";
            default -> flag;
        };
    }

    /**
     * Determine risk level from score (for Claude-friendly output)
     */
    public static String deriveRiskLevelLabel(double riskScore) {
        if (riskScore >= 0.7) return "HIGH";
        if (riskScore >= 0.4) return "MEDIUM";
        return "LOW";
    }

    /**
     * Get next steps for a transaction based on risk
     */
    public static String getNextSteps(FraudQueryResponse.FraudResult result) {
        String riskLevel = deriveRiskLevelLabel(result.getRiskScore());
        double amount = result.getAmount();

        return switch (riskLevel) {
            case "HIGH" -> {
                if (amount >= 100_000) {
                    yield """
                          • Immediate review recommended — validate with account holder before settlement
                          • Check for recent unauthorized access or credential compromise
                          • Verify beneficiary details match account's historical patterns""";
                } else {
                    yield """
                          • Flag for analyst review — assess risk indicators
                          • Collect device/IP and MFA activity for the transaction""";
                }
            }
            case "MEDIUM" -> {
                if (amount > 50_000) {
                    yield """
                          • Review beneficiary and recent account activity
                          • Confirm with account holder if first-time payee or unusual routing""";
                } else {
                    yield """
                          • Monitor transaction — no immediate hold required
                          • Monitor account for related suspicious activity""";
                }
            }
            default -> "• No action required — transaction accepted";
        };
    }

    /**
     * Helper to safely extract double from map
     */
    private static double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Inner class to hold a reason and its source
     */
    public static class ReasonWithSource {
        public final String reason;
        public final String source;

        public ReasonWithSource(String reason, String source) {
            this.reason = reason;
            this.source = source;
        }

        public String formatted() {
            return String.format("• %s — Source: %s", reason, source);
        }
    }
}

