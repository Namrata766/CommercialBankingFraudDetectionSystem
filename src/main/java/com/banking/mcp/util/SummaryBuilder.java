package com.banking.mcp.util;

import com.banking.mcp.mcp.dto.FraudQueryResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SummaryBuilder {

    public static FraudQueryResponse.Summary build(List<FraudQueryResponse.FraudResult> results) {

        int total = results.size();

        long high = results.stream().filter(r -> "HIGH".equals(r.getRiskLevel())).count();
        long medium = results.stream().filter(r -> "MEDIUM".equals(r.getRiskLevel())).count();
        long low = results.stream().filter(r -> "LOW".equals(r.getRiskLevel())).count();

        double avg = results.stream()
                .mapToDouble(FraudQueryResponse.FraudResult::getRiskScore)
                .average()
                .orElse(0);

        Map<String, Long> railDist = results.stream()
                .collect(Collectors.groupingBy(
                        FraudQueryResponse.FraudResult::getRail,
                        Collectors.counting()
                ));

        return new FraudQueryResponse.Summary(
                total,
                (int) high,
                (int) medium,
                (int) low,
                avg,
                railDist
        );
    }
}