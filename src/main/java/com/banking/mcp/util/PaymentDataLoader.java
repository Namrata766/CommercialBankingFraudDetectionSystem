package com.banking.mcp.util;

import com.banking.mcp.model.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@ConditionalOnProperty(name = "app.db.seed-data", havingValue = "true")
public class PaymentDataLoader implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;
    private final Random random = new Random();

    public PaymentDataLoader(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        System.out.println(">>> Starting Comprehensive Payment Seeding...");
        String[] rails = {"WIRE", "ACH", "INSTANT"};
        LocalDate today = LocalDate.now();

        for (String rail : rails) {
            String collectionName = rail.toLowerCase() + "_payments";

            if (!mongoTemplate.collectionExists(collectionName)) {
                mongoTemplate.createCollection(collectionName);
            } else {
                mongoTemplate.remove(new Query(), collectionName);
            }

            List<PaymentDocument> bundle = new ArrayList<>();

            for (int day = 1; day <= 30; day++) {
                LocalDate date = today.minusDays(day);
                for (int i = 1; i <= 5; i++) {
                    bundle.add(createStandardPayment(rail, date, i));
                }
            }

            bundle.add(createRejectedAnomaly(rail, today));
            bundle.add(createHighRiskJurisdictionAnomaly(rail, today));
            bundle.add(createNightShiftAnomaly(rail, today));

            mongoTemplate.insert(bundle, collectionName);
            System.out.println("✅ Seeded " + bundle.size() + " full-schema records into " + collectionName);
        }
    }

    private PaymentDocument createStandardPayment(String rail, LocalDate date, int i) {
        PaymentDocument p = new PaymentDocument();

        // Basic Identifiers
        p.setPaymentId("PMT-" + rail + "-" + System.nanoTime());
        p.setRail(rail);
        p.setType("CREDIT"); // Standard outgoing payment
        p.setStatus("COMPLETED");
        p.setAmount(1200.0 + (i * 45));
        p.setCurrency("USD");

        // Timestamps (Combined for AI logic)
        LocalTime businessTime = LocalTime.of(9 + random.nextInt(8), random.nextInt(60));
        p.setEventTimestamp(date.atTime(businessTime).toInstant(ZoneOffset.UTC));
        p.setExecutionDate(date.toString()); // String helper field

        // Parties (Previously missing)
        p.setDebtor(new Party("GLOBAL_CORP_USA_" + i, "ACC-DBT-100" + i));
        p.setCreditor(new Party("SUPPLIER_SERVICES_" + i, "ACC-CRD-500" + i));

        // Banks (Structured)
        p.setDebtorBank(new BankInfo("WELLS FARGO NY", "WFCUS33", "USA", "300 Park Ave, NY"));
        p.setCreditorBank(new BankInfo("CITIBANK LONDON", "CITIGB2L", "UK", "Canary Wharf, London"));

        // Routing Details (New enrichment)
        p.setChannel(rail.equals("ACH") ? "BATCH" : "ONLINE");
        p.setNetwork(switch (rail) {
            case "WIRE" -> "SWIFT";
            case "ACH" -> "NACHA";
            case "INSTANT" -> "FEDNOW";
            default -> "INTERNAL";
        });

        p.setFlags(new Flags(true, false, false)); // STP=true, Sanction=false, Mod=false

        // Optional: Error fields initialized to null/empty for standard payments
        p.setErrorCode(null);
        p.setErrorDescription(null);

        return p;
    }

    private PaymentDocument createRejectedAnomaly(String rail, LocalDate date) {
        PaymentDocument p = createStandardPayment(rail, date, 888);
        p.setPaymentId("PMT-REJECT-PROBE-" + rail);
        p.setStatus("REJECTED");
        p.setAmount(1.25);
        p.setErrorCode("AC04");
        p.setErrorDescription("ClosedAccountNumber");
        return p;
    }

    private PaymentDocument createHighRiskJurisdictionAnomaly(String rail, LocalDate date) {
        PaymentDocument p = createStandardPayment(rail, date, 999);
        p.setPaymentId("PMT-OFFSHORE-" + rail);
        p.setAmount(95000.00);

        // High risk: Change to offshore creditor
        p.setCreditor(new Party("SHELL_HOLDINGS_LTD", "ACC-KY-999"));
        p.setCreditorBank(new BankInfo("GRAND CAYMAN BANK", "GCMNKYKY", "CAYMAN ISLANDS", "George Town, KY"));

        p.getFlags().setIsModified(true);
        p.getFlags().setIsSTP(false);
        return p;
    }

    private PaymentDocument createNightShiftAnomaly(String rail, LocalDate date) {
        PaymentDocument p = createStandardPayment(rail, date, 111);
        p.setPaymentId("PMT-NIGHT-ALRT-" + rail);
        p.setEventTimestamp(date.atTime(3, 15).toInstant(ZoneOffset.UTC));
        p.setChannel("MOBILE"); // Anomalous for high value corporate
        p.setAmount(25000.00);
        p.setStatus("PENDING");
        return p;
    }
}