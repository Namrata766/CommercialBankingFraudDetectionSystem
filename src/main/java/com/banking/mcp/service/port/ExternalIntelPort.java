package com.banking.mcp.service.port;

import com.banking.mcp.model.evaluation.EwsReputationScore;

public interface ExternalIntelPort {

    EwsReputationScore fetchReputation(String accountId);
}