package com.banking.mcp.repository;

import com.banking.mcp.model.PaymentDocument;

import java.time.Instant;
import java.util.List;

public interface CustomPaymentRepository {

    List<PaymentDocument> findByRailAndDate(String rail, String date);

    List<PaymentDocument> findHistoryByAccount(String accountId, Instant from);

}
