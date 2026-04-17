package com.banking.mcp.service.impl;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.RuleExecutionResult;
import com.banking.mcp.model.evaluation.TriggeredRule;
import com.banking.mcp.service.port.RulesEnginePort;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class RulesEngineService implements RulesEnginePort {

    // Rule thresholds by rail
    private static final double WIRE_HIGH_VALUE_THRESHOLD = 100_000.0;      // $100K
    private static final double ACH_HIGH_VALUE_THRESHOLD = 50_000.0;        // $50K
    private static final double INSTANT_HIGH_VALUE_THRESHOLD = 25_000.0;    // $25K

    // Business hours (9 AM to 5 PM)
    private static final LocalTime BUSINESS_START = LocalTime.of(9, 0);
    private static final LocalTime BUSINESS_END = LocalTime.of(17, 0);

    // High-risk country BICs
    private static final Set<String> HIGH_RISK_BICS = Set.of(
            "CAYAXX22", "BAHAXX22", "CAYMBB22", "PANXXX22", "COSTAXX22"
    );

    @Override
    public RuleExecutionResult evaluate(PaymentDocument payment) {
        RuleExecutionResult result = new RuleExecutionResult();
        List<TriggeredRule> triggeredRules = new ArrayList<>();
        double aggregateScore = 0.0;

        if (payment == null) {
            result.setTriggeredRules(List.of());
            result.setAggregateRuleScore(0.0);
            return result;
        }

        // 🔷 HIGH VALUE RULES (Rail-specific)
        TriggeredRule highValueRule = checkHighValue(payment);
        if (highValueRule != null) {
            triggeredRules.add(highValueRule);
            aggregateScore += highValueRule.getWeight();
        }

        // 🔷 AML/SANCTIONS RULES
        TriggeredRule sanctionRule = checkSanctionHit(payment);
        if (sanctionRule != null) {
            triggeredRules.add(sanctionRule);
            aggregateScore += sanctionRule.getWeight();
        }

        // 🔷 DATA INTEGRITY RULES
        TriggeredRule modifiedDataRule = checkModifiedData(payment);
        if (modifiedDataRule != null) {
            triggeredRules.add(modifiedDataRule);
            aggregateScore += modifiedDataRule.getWeight();
        }

        // 🔷 REJECTED TRANSACTION RULES
        TriggeredRule rejectionRule = checkRejectionPatterns(payment);
        if (rejectionRule != null) {
            triggeredRules.add(rejectionRule);
            aggregateScore += rejectionRule.getWeight();
        }

        // 🔷 OFFSHORE DESTINATION RULES
        TriggeredRule offshoreRule = checkOffshoreDestination(payment);
        if (offshoreRule != null) {
            triggeredRules.add(offshoreRule);
            aggregateScore += offshoreRule.getWeight();
        }

        // 🔷 CHANNEL & TIMING RULES
        TriggeredRule channelTimingRule = checkChannelTiming(payment);
        if (channelTimingRule != null) {
            triggeredRules.add(channelTimingRule);
            aggregateScore += channelTimingRule.getWeight();
        }

        // 🔷 STP BYPASS RULES
        TriggeredRule stpRule = checkStpBypass(payment);
        if (stpRule != null) {
            triggeredRules.add(stpRule);
            aggregateScore += stpRule.getWeight();
        }

        // 🔷 CROSS-BORDER SETTLEMENT RULES
        TriggeredRule crossBorderRule = checkCrossBorderRisk(payment);
        if (crossBorderRule != null) {
            triggeredRules.add(crossBorderRule);
            aggregateScore += crossBorderRule.getWeight();
        }

        // 🔷 Normalize aggregate score (max 1.0)
        aggregateScore = Math.min(aggregateScore, 1.0);

        result.setTriggeredRules(triggeredRules);
        result.setAggregateRuleScore(aggregateScore);

        return result;
    }

    /**
     * RULE_001: High-value transactions (rail-specific thresholds)
     */
    private TriggeredRule checkHighValue(PaymentDocument payment) {
        if (payment.getAmount() == null) return null;

        double threshold = getHighValueThreshold(payment.getRail());
        if (payment.getAmount() >= threshold) {
            double weight = Math.min(0.6, (payment.getAmount() / threshold) * 0.3);
            return new TriggeredRule(
                    "RULE_001",
                    String.format("High-value transaction: $%.2f (Threshold: $%.2f)", payment.getAmount(), threshold),
                    weight
            );
        }
        return null;
    }

    /**
     * RULE_002: Sanctions/AML Hit
     */
    private TriggeredRule checkSanctionHit(PaymentDocument payment) {
        if (payment.getFlags() != null && Boolean.TRUE.equals(payment.getFlags().getIsSanctionHit())) {
            return new TriggeredRule(
                    "RULE_002",
                    "Sanctions/AML watchlist hit detected",
                    0.9  // Very high weight
            );
        }
        return null;
    }

    /**
     * RULE_003: Modified Data (Post-settlement tampering indicator)
     */
    private TriggeredRule checkModifiedData(PaymentDocument payment) {
        if (payment.getFlags() != null && Boolean.TRUE.equals(payment.getFlags().getIsModified())) {
            return new TriggeredRule(
                    "RULE_003",
                    "Payment data modified after submission (BEC risk indicator)",
                    0.75
            );
        }
        return null;
    }

    /**
     * RULE_004: Rejected Transaction (Account Probing)
     */
    private TriggeredRule checkRejectionPatterns(PaymentDocument payment) {
        if ("REJECTED".equalsIgnoreCase(payment.getStatus())) {
            String errorCode = payment.getErrorCode();

            // AC04 = Closed Account (account probing)
            if ("AC04".equals(errorCode)) {
                return new TriggeredRule(
                        "RULE_004",
                        "Account probing detected (AC04: Closed Account rejection)",
                        0.8
                );
            }

            // Multiple rejection codes indicate testing
            if (errorCode != null && (errorCode.startsWith("AC0") || errorCode.startsWith("AG0"))) {
                return new TriggeredRule(
                        "RULE_004",
                        String.format("Systematic rejection pattern detected (Error: %s)", errorCode),
                        0.7
                );
            }
        }
        return null;
    }

    /**
     * RULE_005: Offshore Destination (High-risk country)
     */
    private TriggeredRule checkOffshoreDestination(PaymentDocument payment) {
        if (payment.getCreditorBank() != null) {
            String country = payment.getCreditorBank().getCountry();
            String bic = payment.getCreditorBank().getBic();

            // Check if destination is offshore/high-risk
            if ("CYM".equals(country) || "BHS".equals(country) || "VGB".equals(country) ||
                    "PAN".equals(country) || "CRI".equals(country)) {
                return new TriggeredRule(
                        "RULE_005",
                        String.format("Offshore destination detected: %s (%s)", country, bic),
                        0.6
                );
            }

            // Check high-risk BICs
            if (bic != null && HIGH_RISK_BICS.stream().anyMatch(bic::contains)) {
                return new TriggeredRule(
                        "RULE_005",
                        String.format("High-risk destination BIC: %s", bic),
                        0.65
                );
            }
        }
        return null;
    }

    /**
     * RULE_006: Channel & Timing Anomaly (Mobile outside business hours)
     */
    private TriggeredRule checkChannelTiming(PaymentDocument payment) {
        if (payment.getEventTimestamp() != null && payment.getChannel() != null) {
            LocalTime transactionTime = payment.getEventTimestamp().atZone(ZoneId.of("UTC")).toLocalTime();
            boolean afterHours = transactionTime.isBefore(BUSINESS_START) || transactionTime.isAfter(BUSINESS_END);

            // Mobile after-hours is highly suspicious
            if ("MOBILE".equalsIgnoreCase(payment.getChannel()) && afterHours) {
                return new TriggeredRule(
                        "RULE_006",
                        String.format("After-hours mobile transaction at %s", transactionTime),
                        0.65
                );
            }

            // Wire transfers after hours
            if ("WIRE".equalsIgnoreCase(payment.getRail()) && afterHours) {
                return new TriggeredRule(
                        "RULE_006",
                        String.format("Wire transfer outside business hours at %s", transactionTime),
                        0.5
                );
            }
        }
        return null;
    }

    /**
     * RULE_007: STP Bypass (Manual intervention on straight-through processing)
     */
    private TriggeredRule checkStpBypass(PaymentDocument payment) {
        if (payment.getFlags() != null) {
            Boolean isStp = payment.getFlags().getIsSTP();
            Boolean isModified = payment.getFlags().getIsModified();

            // Manual intervention on normally automated STP
            if (Boolean.TRUE.equals(isStp) && Boolean.TRUE.equals(isModified)) {
                return new TriggeredRule(
                        "RULE_007",
                        "STP bypass: Straight-through processing overridden with manual intervention",
                        0.7
                );
            }
        }
        return null;
    }

    /**
     * RULE_008: Cross-Border Settlement Risk (Different countries)
     */
    private TriggeredRule checkCrossBorderRisk(PaymentDocument payment) {
        if (payment.getDebtorBank() != null && payment.getCreditorBank() != null) {
            String debtorCountry = payment.getDebtorBank().getCountry();
            String creditorCountry = payment.getCreditorBank().getCountry();

            if (debtorCountry != null && creditorCountry != null && !debtorCountry.equals(creditorCountry)) {
                // High-value cross-border to high-risk jurisdiction
                if (payment.getAmount() != null && payment.getAmount() > WIRE_HIGH_VALUE_THRESHOLD) {
                    if (isHighRiskCountry(creditorCountry)) {
                        return new TriggeredRule(
                                "RULE_008",
                                String.format("High-value cross-border to high-risk jurisdiction: %s → %s ($%.2f)",
                                        debtorCountry, creditorCountry, payment.getAmount()),
                                0.75
                        );
                    }
                }
                // Regular cross-border
                return new TriggeredRule(
                        "RULE_008",
                        String.format("Cross-border settlement: %s → %s", debtorCountry, creditorCountry),
                        0.3
                );
            }
        }
        return null;
    }

    /**
     * Helper: Get high-value threshold based on rail
     */
    private double getHighValueThreshold(String rail) {
        if (rail == null) return WIRE_HIGH_VALUE_THRESHOLD;

        return switch (rail.toUpperCase()) {
            case "WIRE" -> WIRE_HIGH_VALUE_THRESHOLD;
            case "ACH" -> ACH_HIGH_VALUE_THRESHOLD;
            case "INSTANT" -> INSTANT_HIGH_VALUE_THRESHOLD;
            default -> WIRE_HIGH_VALUE_THRESHOLD;
        };
    }

    /**
     * Helper: Check if country is high-risk
     */
    private boolean isHighRiskCountry(String country) {
        Set<String> highRiskCountries = Set.of(
                "CYM", "BHS", "VGB", "TCA", "JEY", "GGY", "IMN", // Caribbean tax havens
                "PAN", "CRI", "URY", "CHL", // Central America
                "ARE"  // UAE (depends on policy)
        );
        return highRiskCountries.contains(country);
    }
}