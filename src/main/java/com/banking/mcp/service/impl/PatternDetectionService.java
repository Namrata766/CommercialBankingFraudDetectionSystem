package com.banking.mcp.service.impl;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.BatchPatternAnalysis;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatternDetectionService {

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

        // 🔷 Flags
        List<String> flags = new ArrayList<>();

        if (smurfing) flags.add("SMURFING_PATTERN");
        if (sameBeneficiary) flags.add("SAME_BENEFICIARY");
        if (amountProgression) flags.add("AMOUNT_PROGRESSION");
        if (burst) flags.add("BURST_ACTIVITY");

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

        return maxGap < (5 * 60 * 1000);
    }
}