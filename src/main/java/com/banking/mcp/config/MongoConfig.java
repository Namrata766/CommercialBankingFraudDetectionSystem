package com.banking.mcp.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.lang.NonNull;

import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String atlas_uri;

    private static final String DB_NAME = "fraud_detection_db";

    @Override
    @NonNull
    protected String getDatabaseName() {
        return DB_NAME;
    }

    @Override
    @NonNull
    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(atlas_uri);
        
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToSocketSettings(builder -> 
                    builder.connectTimeout(10, TimeUnit.SECONDS)) // Essential for Cloud Latency
                .build();
        
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        // This ensures the template is explicitly tied to the Atlas client
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }
    
    @Override
    protected boolean autoIndexCreation() {
        return true; // Useful for POC to auto-generate indexes for paymentId
    }
}