package com.banking.mcp.mcp.dto;

import com.banking.mcp.model.evaluation.FinalRiskAssessment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FraudQueryResponse {

    // 🔷 Main results
    private List<FraudResult> results;

    // 🔷 Summary (for dashboard / LLM)
    private Summary summary;

    // 🔷 Metadata
    private Metadata metadata;

    // 🔷 Optional explanation (LLM generated)
    private String explanation;

    // ================= INNER CLASSES =================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudResult {

        private String paymentId;
        private String rail;

        private double amount;
        private String currency;

        private String debtorAccountMasked;
        private String creditorAccountMasked;

        private double riskScore;
        private String riskLevel;

        private List<String> reasonCodes;

        // 🔷 Key insights for UI
        private Map<String, Object> highlights;

        // 🔷 Drill-down data (non-sensitive)
        private Map<String, Object> analyticalSummary;
    }

    // 🔷 Aggregated summary
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Summary {

        private int totalTransactions;

        // 🔷 Fix for your current error
        private double averageRisk;

        // 🔷 Pattern intelligence
        private List<String> patternFlags;

        // 🔷 Flexible analytical data
        private Map<String, Object> additionalInsights;
    }

    // 🔷 Execution metadata
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {

        private long executionTimeMs;
        private String requestId;

        private List<String> appliedFilters;

        private boolean partialResults;
    }
}