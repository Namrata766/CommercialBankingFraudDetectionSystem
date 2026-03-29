package com.banking.mcp.service.impl;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.repository.CustomPaymentRepository;
import com.banking.mcp.service.port.TransactionFetchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionFetchService implements TransactionFetchPort {

    private final CustomPaymentRepository repository;

    @Override
    public List<PaymentDocument> fetchTransactions(String rail, String date) {

        log.info("Fetching transactions for rail: {} and date: {}", rail, date);

        if (rail == null || date == null) {
            log.warn("Rail or date is null. Returning empty result.");
            return Collections.emptyList();
        }

        try {
            List<PaymentDocument> results =
                    repository.findByRailAndDate(rail, date);

            log.info("Fetched {} transactions for rail={} date={}",
                    results.size(), rail, date);

            return results;

        } catch (Exception e) {
            log.error("Error fetching transactions for rail={} date={}", rail, date, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<PaymentDocument> fetchHistory(String accountId, int days) {

        log.info("Fetching {} days history for accountId={}", days, accountId);

        if (accountId == null || days <= 0) {
            log.warn("Invalid accountId or days. Returning empty history.");
            return Collections.emptyList();
        }

        try {
            Instant from = Instant.now().minus(days, ChronoUnit.DAYS);

            List<PaymentDocument> history =
                    repository.findHistoryByAccount(accountId, from);

            log.info("Fetched {} historical transactions for accountId={}",
                    history.size(), accountId);

            return history;

        } catch (Exception e) {
            log.error("Error fetching history for accountId={}", accountId, e);
            return Collections.emptyList();
        }
    }
}