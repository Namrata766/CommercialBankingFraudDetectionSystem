package com.banking.mcp.service.port;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.AnomalyScore;

import java.util.List;

public interface AnomalyDetectionPort {

    List<AnomalyScore> calculateDeviation(
            List<PaymentDocument> current,
            List<PaymentDocument> history
    );
}
