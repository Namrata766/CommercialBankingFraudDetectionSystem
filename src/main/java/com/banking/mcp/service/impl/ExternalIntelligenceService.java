package com.banking.mcp.service.impl;

import com.banking.mcp.model.evaluation.EwsReputationScore;
import com.banking.mcp.service.port.ExternalIntelPort;
import org.springframework.stereotype.Service;

@Service
public class ExternalIntelligenceService implements ExternalIntelPort {

    @Override
    public EwsReputationScore fetchReputation(String accountId) {

        EwsReputationScore score = new EwsReputationScore();
        score.setAccountReputationScore(75);
        score.setReportedFraudulent(false);
        score.setSource("MockEWS");

        return score;
    }
}