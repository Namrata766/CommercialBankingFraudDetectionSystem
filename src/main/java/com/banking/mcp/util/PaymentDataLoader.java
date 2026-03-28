package com.banking.mcp.util;

import com.banking.mcp.model.Flags;
import com.banking.mcp.model.Party;
import com.banking.mcp.model.PaymentDocument;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query; // Added for clearing
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.db.seed-data", havingValue = "true")
public class PaymentDataLoader implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    public PaymentDataLoader(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        System.out.println(">>> Starting Smart Payment Data Seeding...");

        String[] rails = {"WIRE", "ACH", "INSTANT"};
        LocalDate today = LocalDate.now();

        for (String rail : rails) {
            String collectionName = rail.toLowerCase() + "_payments";

            // 1. Check if collection exists; Create if missing, Clear if exists
            if (!mongoTemplate.collectionExists(collectionName)) {
                mongoTemplate.createCollection(collectionName);
                System.out.println("Created new collection: " + collectionName);
            } else {
                // remove(new Query()) acts like 'DELETE FROM table' (keeps indexes)
                mongoTemplate.remove(new Query(), collectionName);
                System.out.println("Cleared existing data from: " + collectionName);
            }

            List<PaymentDocument> bundle = new ArrayList<>();

            // 2. Generate 30 Days of History (Baseline Patterns)
            // Reducing to 5 per day for faster Atlas seeding in a POC
            for (int day = 1; day <= 30; day++) {
                LocalDate executionDate = today.minusDays(day);
                for (int i = 1; i <= 5; i++) {
                    bundle.add(createStandardPayment(rail, executionDate, i));
                }
            }

            // 3. Inject Today's Anomaly (The "Fraud" Case)
            bundle.add(createAnomaly(rail, today));

            // 4. Batch Insert for Performance
            mongoTemplate.insert(bundle, collectionName);
            System.out.println("✅ Seeded " + bundle.size() + " records into " + collectionName);
        }
    }

    private PaymentDocument createStandardPayment(String rail, LocalDate date, int index) {
        PaymentDocument p = new PaymentDocument();
        p.setPaymentId("PMT-" + rail + "-" + date.toString().replace("-", "") + "-" + index);
        p.setRail(rail);
        p.setExecutionDate(date.toString());
        p.setAmount(rail.equals("WIRE") ? 5000.0 + (index * 10) : 100.0 + index);
        p.setCurrency("USD");
        p.setChannel(rail.equals("ACH") ? "BATCH" : "ONLINE");
        p.setStatus("COMPLETED");

        p.setDebtor(new Party("CORP_SENDER_" + index, "ACC-DBT-" + index));
        p.setCreditor(new Party("SUPPLIER_" + index, "ACC-CRD-" + index));

        p.setFlags(new Flags(true, false, false));
        return p;
    }

    private PaymentDocument createAnomaly(String rail, LocalDate date) {
        PaymentDocument p = createStandardPayment(rail, date, 999);
        p.setPaymentId("PMT-ANOMALY-" + rail);
        p.setStatus("PENDING");

        switch (rail) {
            case "WIRE" -> {
                p.setAmount(500000.00);
                p.setFlags(new Flags(true, false, true)); // isModified = true
                p.setCreditor(new Party("UNKNOWN_OFFSHORE_CORP", "ACC-HIDDEN-123"));
            }
            case "INSTANT" -> {
                p.setFlags(new Flags(true, true, false)); // isSanctionHit = true
                p.setChannel("MOBILE");
            }
            case "ACH" -> {
                p.setAmount(25000.00);
                p.setChannel("WEB_PORTAL");
            }
        }
        return p;
    }
}