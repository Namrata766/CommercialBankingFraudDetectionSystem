package com.banking.mcp.service.port;

import com.banking.mcp.model.Party;
import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.CounterpartyProfile;

import java.util.List;

public interface CounterpartyAnalysisPort {

    CounterpartyProfile analyze(
            Party debtor,
            Party creditor,
            List<PaymentDocument> history
    );
}