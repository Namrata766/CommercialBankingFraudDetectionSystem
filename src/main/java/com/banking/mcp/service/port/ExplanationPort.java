package com.banking.mcp.service.port;

import com.banking.mcp.model.evaluation.FinalRiskAssessment;

public interface ExplanationPort {

    String generateExplanation(FinalRiskAssessment assessment);
}
