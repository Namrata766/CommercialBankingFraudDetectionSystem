package com.banking.mcp.util;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.FinalRiskAssessment;
import com.banking.mcp.mcp.dto.FraudQueryResponse;

import java.util.HashMap;
import java.util.Map;

public class FraudResultMapper {

    public static FraudQueryResponse.FraudResult map(
            PaymentDocument txn,
            FinalRiskAssessment risk
    ) {

        FraudQueryResponse.FraudResult result =
                new FraudQueryResponse.FraudResult();

        result.setPaymentId(txn.getPaymentId());
        result.setRail(txn.getRail());

        result.setAmount(txn.getAmount());
        result.setCurrency(txn.getCurrency());

        result.setDebtorAccountMasked(
                MaskingUtil.maskAccount(txn.getDebtor().getAccountId())
        );

        result.setCreditorAccountMasked(
                MaskingUtil.maskAccount(txn.getCreditor().getAccountId())
        );

        result.setRiskScore(risk.getFinalRiskScore());
        result.setRiskLevel(risk.getRiskLevel());
        result.setReasonCodes(risk.getReasonCodes());

        Map<String, Object> highlights = new HashMap<>();
        highlights.put("anomalyScore", risk.getAnalyticalSummary().getOrDefault("anomalyScore", 0));
        highlights.put("ruleScore", risk.getAnalyticalSummary().getOrDefault("ruleScore", 0));
        result.setHighlights(highlights);

        result.setAnalyticalSummary(risk.getAnalyticalSummary());

        return result;
    }
}