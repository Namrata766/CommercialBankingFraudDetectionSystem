package com.banking.mcp.service.port;

import com.banking.mcp.model.PaymentDocument;

import java.util.List;

public interface TransactionFetchPort {

    List<PaymentDocument> fetchTransactions(String rail, String date);

    List<PaymentDocument> fetchHistory(String accountId, int days);
}
