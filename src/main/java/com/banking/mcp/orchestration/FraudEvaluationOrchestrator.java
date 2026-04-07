package com.banking.mcp.orchestration;

import com.banking.mcp.mcp.dto.FraudQueryRequest;
import com.banking.mcp.mcp.dto.FraudQueryResponse;
import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.*;
import com.banking.mcp.service.impl.LlmExplanationService;
import com.banking.mcp.service.impl.PatternDetectionService;
import com.banking.mcp.service.port.*;
import com.banking.mcp.util.FraudResultMapper;
import com.banking.mcp.util.SummaryBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
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
    private final PatternDetectionService patternService;
    private final LlmExplanationService explanationService;

    public FraudEvaluationOrchestrator(
            TransactionFetchPort fetchService,
            AnomalyDetectionPort anomalyService,
            RulesEnginePort rulesService,
            CounterpartyAnalysisPort counterpartyService,
            ExternalIntelPort externalService,
            RiskScoringPort scoringService,
            PatternDetectionService patternService,
            LlmExplanationService explanationService
    ) {
        this.fetchService = fetchService;
        this.anomalyService = anomalyService;
        this.rulesService = rulesService;
        this.counterpartyService = counterpartyService;
        this.externalService = externalService;
        this.scoringService = scoringService;
        this.patternService = patternService;
        this.explanationService = explanationService;
    }

    /**
     * Entry point for structured fraud evaluation
     */
    public String evaluateStructured(String userQuery) throws JsonProcessingException {
        FraudQueryRequest request = parseQuery(userQuery);
        FraudQueryResponse response = execute(request);
        return explanationService.generateExplanation(response);
    }

    /**
     * Core orchestration logic (scatter-gather)
     */
    public FraudQueryResponse execute(FraudQueryRequest request) {

        long start = System.currentTimeMillis();

        List<PaymentDocument> txns =
                fetchService.fetchTransactions(request.getRail(), request.getDate());

        // 🔷 Compute pattern ONCE (batch-level)
        BatchPatternAnalysis patternAnalysis = patternService.analyze(txns);

        List<CompletableFuture<FinalRiskAssessment>> futures = txns.stream()
                .map(txn -> processTransaction(txn, patternAnalysis))
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

        // 🔷 Summary with pattern intelligence
        response.setSummary(
                SummaryBuilder.build(results, patternAnalysis)
        );

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
    private CompletableFuture<FinalRiskAssessment> processTransaction(
            PaymentDocument txn,
            BatchPatternAnalysis patternAnalysis
    ) {

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
                        ewsFuture.join(),
                        patternAnalysis   // 🔥 NEW
                )
        );
    }

    /**
     * Improved query parser with real date parsing
     */
    private FraudQueryRequest parseQuery(String query) {

        FraudQueryRequest request = new FraudQueryRequest();

        String lower = query.toLowerCase();

        // 🔷 Rail detection
        if (lower.contains("wire")) {
            request.setRail("wire");
        } else if (lower.contains("ach")) {
            request.setRail("ach");
        } else if (lower.contains("instant")) {
            request.setRail("instant");
        }

        // 🔷 Date parsing
        request.setDate(extractDate(query));

        return request;
    }

    /**
     * Extracts date from natural language query
     */
    private String extractDate(String query) {

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,                 // 2026-03-28
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),        // 28-03-2026
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),        // 28/03/2026
                DateTimeFormatter.ofPattern("MMMM d yyyy"),       // March 28 2026
                DateTimeFormatter.ofPattern("d MMMM yyyy")        // 28 March 2026
        );

        for (String token : query.split(" ")) {
            for (DateTimeFormatter formatter : formatters) {
                try {
                    LocalDate date = LocalDate.parse(token.trim(), formatter);
                    return date.toString(); // ISO format
                } catch (DateTimeParseException ignored) {}
            }
        }

        // 🔷 Fallback: today
        return LocalDate.now().toString();
    }
}