package com.banking.mcp.service.impl;

import com.banking.mcp.model.Party;
import com.banking.mcp.model.PaymentDocument;
import com.banking.mcp.model.evaluation.CounterpartyProfile;
import com.banking.mcp.service.port.CounterpartyAnalysisPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CounterpartyAnalysisService implements CounterpartyAnalysisPort {

    @Override
    public CounterpartyProfile analyze(Party debtor, Party creditor, List<PaymentDocument> history) {

        CounterpartyProfile profile = new CounterpartyProfile();

        profile.setFirstTimeInteraction(true);
        profile.setRelationshipRisk("MEDIUM");
        profile.setCountryRiskLevel("LOW");

        return profile;
    }
}