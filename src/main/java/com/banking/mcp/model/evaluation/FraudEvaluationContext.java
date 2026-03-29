package com.banking.mcp.model.evaluation;

import com.banking.mcp.model.PaymentDocument;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FraudEvaluationContext {
    private PaymentDocument targetTransaction;
    private List<PaymentDocument> history30Days;
}
