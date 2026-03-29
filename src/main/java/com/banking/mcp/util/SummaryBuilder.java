package com.banking.mcp.util;

import com.banking.mcp.mcp.dto.FraudQueryResponse;
import com.banking.mcp.model.evaluation.BatchPatternAnalysis;

import java.util.List;
import java.util.Map;

public class SummaryBuilder {

    public static FraudQueryResponse.Summary build(
            List<FraudQueryResponse.FraudResult> results,
            BatchPatternAnalysis pattern
    ) {

        FraudQueryResponse.Summary summary = new FraudQueryResponse.Summary();

        summary.setTotalTransactions(results.size());

        double avgRisk = results.stream()
                .mapToDouble(FraudQueryResponse.FraudResult::getRiskScore)
                .average()
                .orElse(0);

        summary.setAverageRisk(avgRisk);

        summary.setPatternFlags(pattern.getPatternFlags());

        summary.setAdditionalInsights(Map.of(
                "avgAmount", pattern.getAvgAmount(),
                "stdDeviation", pattern.getStdDeviation()
        ));

        return summary;
    }
}