package com.banking.mcp.orchestration;

import com.banking.mcp.mcp.dto.FraudQueryRequest;
import com.banking.mcp.mcp.dto.FraudQueryResponse;
import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.*;
import com.banking.mcp.service.port.*;
import com.banking.mcp.util.FraudResultMapper;
import com.banking.mcp.util.SummaryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Service
public class FraudEvaluationOrchestrator {

    private final TransactionFetchPort fetchService;
    private final AnomalyDetectionPort anomalyService;
    private final RulesEnginePort rulesService;
    private final CounterpartyAnalysisPort counterpartyService;
    private final ExternalIntelPort externalService;
    private final RiskScoringPort scoringService;

    public FraudEvaluationOrchestrator(
            TransactionFetchPort fetchService,
            AnomalyDetectionPort anomalyService,
            RulesEnginePort rulesService,
            CounterpartyAnalysisPort counterpartyService,
            ExternalIntelPort externalService,
            RiskScoringPort scoringService
    ) {
        this.fetchService = fetchService;
        this.anomalyService = anomalyService;
        this.rulesService = rulesService;
        this.counterpartyService = counterpartyService;
        this.externalService = externalService;
        this.scoringService = scoringService;
    }

    /**
     * Entry point for structured fraud evaluation
     */
    public FraudQueryResponse evaluateStructured(String userQuery) {

        FraudQueryRequest request = parseQuery(userQuery);
        return execute(request);
    }

    /**
     * Core orchestration logic (scatter-gather)
     */
    public FraudQueryResponse execute(FraudQueryRequest request) {

        long start = System.currentTimeMillis();

        List<PaymentDocument> txns =
                fetchService.fetchTransactions(request.getRail(), request.getDate());

        List<CompletableFuture<FinalRiskAssessment>> futures = txns.stream()
                .map(this::processTransaction)
                .toList();

        List<FinalRiskAssessment> riskResults =
                futures.stream()
                        .map(CompletableFuture::join)
                        .toList();

        // 🔷 Map to response DTO
        List<FraudQueryResponse.FraudResult> results =
                IntStream.range(0, txns.size())
                        .mapToObj(i ->
                                FraudResultMapper.map(txns.get(i), riskResults.get(i))
                        )
                        .toList();

        FraudQueryResponse response = new FraudQueryResponse();
        response.setResults(results);

        // 🔷 Summary
        response.setSummary(SummaryBuilder.build(results));

        // 🔷 Metadata
        response.setMetadata(new FraudQueryResponse.Metadata(
                System.currentTimeMillis() - start,
                UUID.randomUUID().toString(),
                List.of("rail=" + request.getRail(), "date=" + request.getDate()),
                false
        ));

        return response;
    }

    /**
     * Parallel processing per transaction
     */
    private CompletableFuture<FinalRiskAssessment> processTransaction(PaymentDocument txn) {

        CompletableFuture<List<PaymentDocument>> historyFuture =
                CompletableFuture.supplyAsync(() ->
                        fetchService.fetchHistory(txn.getDebtor().getAccountId(), 30)
                );

        CompletableFuture<AnomalyScore> anomalyFuture =
                historyFuture.thenApplyAsync(history ->
                        anomalyService.calculateDeviation(List.of(txn), history).get(0)
                );

        CompletableFuture<RuleExecutionResult> rulesFuture =
                CompletableFuture.supplyAsync(() ->
                        rulesService.evaluate(txn)
                );

        CompletableFuture<CounterpartyProfile> counterpartyFuture =
                historyFuture.thenApplyAsync(history ->
                        counterpartyService.analyze(txn.getDebtor(), txn.getCreditor(), history)
                );

        CompletableFuture<EwsReputationScore> ewsFuture =
                CompletableFuture.supplyAsync(() ->
                        externalService.fetchReputation(txn.getCreditor().getAccountId())
                );

        return CompletableFuture.allOf(
                anomalyFuture, rulesFuture, counterpartyFuture, ewsFuture
        ).thenApply(v ->
                scoringService.aggregate(
                        txn,
                        anomalyFuture.join(),
                        rulesFuture.join(),
                        counterpartyFuture.join(),
                        ewsFuture.join()
                )
        );
    }

    /**
     * Temporary query parser (replace later with LLM structured extraction)
     */
    private FraudQueryRequest parseQuery(String query) {

        FraudQueryRequest request = new FraudQueryRequest();

        String lower = query.toLowerCase();

        if (lower.contains("wire")) {
            request.setRail("wire");
        } else if (lower.contains("ach")) {
            request.setRail("ach");
        } else if (lower.contains("instant")) {
            request.setRail("instant");
        }

        // TODO: Replace with real date parsing
        request.setDate("2026-03-28");

        return request;
    }
}