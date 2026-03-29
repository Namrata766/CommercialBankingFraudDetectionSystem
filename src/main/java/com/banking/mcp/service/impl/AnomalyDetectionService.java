package com.banking.mcp.service.impl;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.AnomalyScore;

import java.util.List;

import com.banking.mcp.service.port.AnomalyDetectionPort;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnomalyDetectionService implements AnomalyDetectionPort {

    @Override
    public List<AnomalyScore> calculateDeviation(
            List<PaymentDocument> current,
            List<PaymentDocument> history
    ) {

        if (history == null || history.isEmpty()) {
            return current.stream()
                    .map(txn -> new AnomalyScore(txn.getPaymentId(), 0.5, List.of("No history available")))
                    .toList();
        }

        double mean = history.stream()
                .mapToDouble(PaymentDocument::getAmount)
                .average()
                .orElse(0);

        double stdDev = calculateStdDev(history, mean);

        return current.stream()
                .map(txn -> evaluate(txn, mean, stdDev, history))
                .collect(Collectors.toList());
    }

    private AnomalyScore evaluate(
            PaymentDocument txn,
            double mean,
            double stdDev,
            List<PaymentDocument> history
    ) {

        List<String> deviations = new ArrayList<>();
        double score = 0.0;

        // 🔷 Amount anomaly (Z-score)
        double z = stdDev == 0 ? 0 : (txn.getAmount() - mean) / stdDev;

        if (Math.abs(z) > 2) {
            deviations.add("Amount significantly deviates from historical mean");
            score += 0.4;
        }

        // 🔷 High amount spike
        if (txn.getAmount() > mean * 3) {
            deviations.add("Amount > 3x historical average");
            score += 0.3;
        }

        // 🔷 Off-hours detection
        int hour = txn.getEventTimestamp()
                .atZone(ZoneId.systemDefault())
                .getHour();

        if (hour < 6 || hour > 22) {
            deviations.add("Transaction during unusual hours");
            score += 0.2;
        }

        // 🔷 First-time recipient
        boolean knownRecipient = history.stream()
                .anyMatch(h -> h.getCreditor().getAccountId()
                        .equals(txn.getCreditor().getAccountId()));

        if (!knownRecipient) {
            deviations.add("First-time recipient");
            score += 0.3;
        }

        score = Math.min(score, 1.0);

        return new AnomalyScore(txn.getPaymentId(), score, deviations);
    }

    private double calculateStdDev(List<PaymentDocument> history, double mean) {

        double variance = history.stream()
                .mapToDouble(txn -> Math.pow(txn.getAmount() - mean, 2))
                .average()
                .orElse(0);

        return Math.sqrt(variance);
    }
}