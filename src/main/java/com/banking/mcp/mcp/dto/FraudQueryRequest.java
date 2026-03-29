package com.banking.mcp.mcp.dto;

import lombok.Data;

import java.util.List;

@Data
public class FraudQueryRequest {

    // 🔷 Core filters (LLM must extract these)
    private String rail;              // wire | ach | instant
    private String date;              // YYYY-MM-DD

    // 🔷 Optional filters (for future NL queries)
    private Double minAmount;
    private Double maxAmount;

    private String currency;
    private String status;            // SUCCESS, FAILED, PENDING

    private String debtorAccountId;
    private String creditorAccountId;

    private List<String> countries;   // jurisdiction filtering

    // 🔷 Behavioral flags
    private Boolean highRiskOnly;     // if user says "show only risky"
    private Boolean includeHistory;   // for deeper analysis

    // 🔷 Pagination (important later)
    private Integer limit = 50;
    private Integer offset = 0;

    // 🔷 Debug / tracing (optional but powerful)
    private Boolean explain = true;   // whether to generate LLM explanation
}