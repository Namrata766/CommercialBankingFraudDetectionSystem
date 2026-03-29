package com.banking.mcp.model.evaluation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchPatternAnalysis {

    private List<String> patternFlags;

    // 🔷 Already likely present
    private double avgAmount;
    private double stdDeviation;

    // 🔥 ADD THIS
    private Double maxAmount;

    // Optional flags
    private boolean possibleSmurfing;
    private boolean timeClustering;
}