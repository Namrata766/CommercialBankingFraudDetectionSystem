package com.banking.mcp.repository.impl;

import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.repository.CustomPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomPaymentRepositoryImpl implements CustomPaymentRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<PaymentDocument> findByRailAndDate(String rail, String date) {

        String collection = resolveCollection(rail);

        Query query = new Query();
        query.addCriteria(Criteria.where("executionDate").is(date));

        return mongoTemplate.find(query, PaymentDocument.class, collection);
    }

    @Override
    public List<PaymentDocument> findHistoryByAccount(String accountId, Instant from) {

        Query query = new Query();

        query.addCriteria(
                new Criteria().andOperator(
                        Criteria.where("eventTimestamp").gte(from),
                        new Criteria().orOperator(
                                Criteria.where("debtor.accountId").is(accountId),
                                Criteria.where("creditor.accountId").is(accountId)
                        )
                )
        );

        // 🔥 Search across ALL collections
        List<PaymentDocument> results = new ArrayList<>();

        for (String collection : List.of("wire_payments", "ach_payments", "instant_payments")) {
            results.addAll(
                    mongoTemplate.find(query, PaymentDocument.class, collection)
            );
        }

        return results;
    }

    private String resolveCollection(String rail) {
        return switch (rail.toLowerCase()) {
            case "wire" -> "wire_payments";
            case "ach" -> "ach_payments";
            case "instant" -> "instant_payments";
            default -> throw new IllegalArgumentException("Invalid rail: " + rail);
        };
    }
}