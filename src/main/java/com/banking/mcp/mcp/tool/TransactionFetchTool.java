package com.banking.mcp.mcp.tool;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.service.port.TransactionFetchPort;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionFetchTool {

    private final TransactionFetchPort service;

    public TransactionFetchTool(TransactionFetchPort service) {
        this.service = service;
    }

    @Tool(
            name = "fetch_transactions",
            description = "Fetch transactions based on rail and date"
    )
    public List<PaymentDocument> fetch(String rail, String date) {
        return service.fetchTransactions(rail, date);
    }
}