package com.banking.mcp.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Document // The collection name will be set dynamically by the loader or repo
public class PaymentDocument {
    @Id
    private String id;
    private String paymentId;
    private String rail;
    private String type;
    private String status;
    private Double amount;
    private String currency;
    private String executionDate;
    private Instant eventTimestamp;
    private Party debtor;
    private Party creditor;
    private String channel;
    private String network;
    private Flags flags;
    private EventDetail event;
    private String errorCode;
    private String errorDescription;
    // Structured Bank Objects for Jurisdictional Risk Analysis
    private BankInfo debtorBank;
    private BankInfo creditorBank;
}
