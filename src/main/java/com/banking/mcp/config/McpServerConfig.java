package com.banking.mcp.config;

import com.banking.mcp.mcp.tool.FraudOrchestratorTool;
import com.banking.mcp.mcp.tool.TransactionFetchTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            FraudOrchestratorTool orchestratorTool,
            TransactionFetchTool transactionFetchTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(orchestratorTool, transactionFetchTool)
                .build();
    }
}