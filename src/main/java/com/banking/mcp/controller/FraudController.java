package com.banking.mcp.controller;

import com.banking.mcp.orchestration.FraudEvaluationOrchestrator;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
@Slf4j
public class FraudController {

    private final FraudEvaluationOrchestrator orchestrator;

    public FraudController(FraudEvaluationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<String> evaluateFraud(@RequestBody FraudRequest request) {
        try {
            log.info("Received fraud evaluation request: {}", request.getQuery());

            // Validate input
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Query cannot be null or empty");
            }

            String result = orchestrator.evaluateStructured(request.getQuery());
            log.info("Fraud evaluation completed successfully, response length: {}", result.length());
            return ResponseEntity.ok(result);
        } catch (JsonProcessingException e) {
            log.error("JSON processing error during fraud evaluation", e);
            return ResponseEntity.internalServerError().body("Error processing fraud evaluation: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during fraud evaluation", e);
            return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Fraud Detection Service is running");
    }

    public static class FraudRequest {
        private String query;

        public FraudRequest() {}

        public FraudRequest(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }
}
