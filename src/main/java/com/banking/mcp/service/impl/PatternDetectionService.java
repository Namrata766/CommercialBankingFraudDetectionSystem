package com.banking.mcp.service.impl;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.BatchPatternAnalysis;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatternDetectionService {

    // Define offshore/high-risk countries
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "CYM", "BHS", "VGB", "TCA", "JEY", "GGY", "IMN", // Cayman, Bahamas, etc.
            "PAN", "CRI", "URY", "CHL" // Panama, etc.
    );

    // Business hours (9 AM to 5 PM)
    private static final LocalTime BUSINESS_START = LocalTime.of(9, 0);
    private static final LocalTime BUSINESS_END = LocalTime.of(17, 0);

    public BatchPatternAnalysis analyze(List<PaymentDocument> txns) {

        if (txns == null || txns.isEmpty()) {
            return new BatchPatternAnalysis(
                    List.of(),   // flags
                    0.0,         // avg
                    0.0,         // stdDev
                    0.0,         // maxAmount
                    false,       // smurfing
                    false        // clustering
            );
        }

        // 🔷 Extract amounts safely
        List<Double> amounts = txns.stream()
                .map(PaymentDocument::getAmount)
                .filter(Objects::nonNull)
                .toList();

        double avg = amounts.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        double variance = amounts.stream()
                .mapToDouble(a -> Math.pow(a - avg, 2))
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(variance);

        double maxAmount = amounts.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        // 🔷 Pattern checks
        boolean sameBeneficiary = txns.stream()
                .map(t -> t.getCreditor() != null ? t.getCreditor().getAccountId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count() == 1;

        boolean amountProgression = isMonotonic(amounts);

        boolean smurfing = stdDev < (avg * 0.1) && txns.size() >= 3;

        boolean burst = isTimeClustered(txns);

        // 🔷 Additional pattern detections
        boolean offshoreDestination = isOffshoreDestination(txns);
        boolean unusualHours = isUnusualHours(txns);
        boolean roundAmounts = isRoundAmounts(amounts);
        boolean sameDebtorDifferentCreditors = isSameDebtorDifferentCreditors(txns);
        boolean highFrequency = isHighFrequency(txns);
        boolean channelAnomaly = isChannelAnomaly(txns);

        // 🔷 Flags
        List<String> flags = new ArrayList<>();

        if (smurfing) flags.add("SMURFING_PATTERN");
        if (sameBeneficiary) flags.add("SAME_BENEFICIARY");
        if (amountProgression) flags.add("AMOUNT_PROGRESSION");
        if (burst) flags.add("BURST_ACTIVITY");
        if (offshoreDestination) flags.add("OFFSHORE_DESTINATION");
        if (unusualHours) flags.add("UNUSUAL_HOURS");
        if (roundAmounts) flags.add("ROUND_AMOUNTS");
        if (sameDebtorDifferentCreditors) flags.add("SAME_DEBTOR_MULTIPLE_CREDITORS");
        if (highFrequency) flags.add("HIGH_FREQUENCY");
        if (channelAnomaly) flags.add("CHANNEL_ANOMALY");

        // 🔷 Use correct constructor (IMPORTANT)
        return new BatchPatternAnalysis(
                flags,
                avg,
                stdDev,
                maxAmount,
                smurfing,
                burst
        );
    }

    private boolean isMonotonic(List<Double> values) {
        if (values.size() < 2) return false;

        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) < values.get(i - 1)) {
                return false;
            }
        }
        return true;
    }

    private boolean isTimeClustered(List<PaymentDocument> txns) {

        List<Long> timestamps = txns.stream()
                .map(PaymentDocument::getEventTimestamp)
                .filter(Objects::nonNull)
                .map(ts -> ts.toEpochMilli())
                .sorted()
                .toList();

        if (timestamps.size() < 2) return false;

        long maxGap = 0;

        for (int i = 1; i < timestamps.size(); i++) {
            long gap = timestamps.get(i) - timestamps.get(i - 1);
            maxGap = Math.max(maxGap, gap);
        }

        return maxGap < (5 * 60 * 1000); // 5 minutes
    }

    private boolean isOffshoreDestination(List<PaymentDocument> txns) {
        return txns.stream()
                .anyMatch(t -> t.getCreditorBank() != null &&
                        HIGH_RISK_COUNTRIES.contains(t.getCreditorBank().getCountry()));
    }

    private boolean isUnusualHours(List<PaymentDocument> txns) {
        return txns.stream()
                .anyMatch(t -> {
                    if (t.getEventTimestamp() == null) return false;
                    LocalTime time = t.getEventTimestamp().atZone(ZoneId.of("UTC")).toLocalTime();
                    return time.isBefore(BUSINESS_START) || time.isAfter(BUSINESS_END);
                });
    }

    private boolean isRoundAmounts(List<Double> amounts) {
        return amounts.stream()
                .anyMatch(amount -> amount % 1000 == 0 && amount >= 1000);
    }

    private boolean isSameDebtorDifferentCreditors(List<PaymentDocument> txns) {
        if (txns.size() < 2) return false;

        String debtorAccount = txns.get(0).getDebtor() != null ? txns.get(0).getDebtor().getAccountId() : null;
        if (debtorAccount == null) return false;

        boolean sameDebtor = txns.stream()
                .allMatch(t -> t.getDebtor() != null && debtorAccount.equals(t.getDebtor().getAccountId()));

        long distinctCreditors = txns.stream()
                .map(t -> t.getCreditor() != null ? t.getCreditor().getAccountId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return sameDebtor && distinctCreditors > 1;
    }

    private boolean isHighFrequency(List<PaymentDocument> txns) {
        if (txns.size() < 5) return false;

        List<Long> timestamps = txns.stream()
                .map(PaymentDocument::getEventTimestamp)
                .filter(Objects::nonNull)
                .map(ts -> ts.toEpochMilli())
                .sorted()
                .toList();

        if (timestamps.size() < 5) return false;

        long timeSpan = timestamps.get(timestamps.size() - 1) - timestamps.get(0);
        long oneHourMs = 60 * 60 * 1000;

        return timeSpan < oneHourMs && txns.size() >= 5;
    }

    private boolean isChannelAnomaly(List<PaymentDocument> txns) {
        // Flag if any transaction uses MOBILE channel outside business hours
        return txns.stream()
                .anyMatch(t -> "MOBILE".equals(t.getChannel()) &&
                        t.getEventTimestamp() != null &&
                        t.getEventTimestamp().atZone(ZoneId.of("UTC")).toLocalTime().isBefore(BUSINESS_START));
    }
}