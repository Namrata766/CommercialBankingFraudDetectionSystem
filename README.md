# 🏦 Commercial Banking Fraud Detection System

This project uses **MongoDB Atlas** to store and analyze payment transactions across three major banking rails. The system includes an advanced automated data seeder designed to simulate 30 days of transaction history plus high-fidelity anomaly cases to test AI reasoning and Rule Engine triggers.

---

## 📊 Collection Architecture

The database `fraud_detection_db` partitions traffic into three primary collections. This allows for rail-specific risk modeling:

| Collection Name | Rail Type | Network | Description |
| :--- | :--- | :--- | :--- |
| `wire_payments` | **WIRE** | SWIFT / FEDWIRE | High-value, real-time gross settlements (RTGS). |
| `ach_payments` | **ACH** | NACHA | Batch-processed low-value clearing house transfers. |
| `instant_payments` | **INSTANT** | FEDNOW / RTP | Real-time retail/corporate payments. |

### Data Schema (`PaymentDocument`)
The schema follows ISO 20022 principles to provide the AI with deep context for fraud analysis:

* **`paymentId`**: Unique transaction reference.
* **`eventTimestamp`**: **BSON Instant** (UTC). The "Source of Truth" for velocity and time-of-day checks.
* **`amount` / `currency`**: Transaction value (e.g., $125,000.00 USD).
* **`debtor` / `creditor`**: Objects containing `name` and `accountId`.
* **`debtorBank` / `creditorBank`**: Structured objects containing **BIC code**, **Country**, and **Address**.
* **`status`**: Current state (`COMPLETED`, `PENDING`, or `REJECTED`).
* **`errorCode` / `errorDescription`**: ISO-standard reject codes (e.g., `AC04` for Closed Account) used to identify account probing.
* **`flags`**:
    * **`isStp`**: Straight-Through Processing (True = fully automated).
    * **`isSanctionHit`**: AML/Watchlist matching hit.
    * **`isModified`**: Indicates data was altered after client submission (BEC risk).

---

## 🚀 Seeding & Data Refresh Logic

The `PaymentDataLoader` executes on application startup. It uses a **"Smart Refresh"** strategy to ensure a clean testing environment without breaking performance optimizations.

### How it works:
1.  **Validation**: Checks if collections exist in the Atlas Cluster.
2.  **The "Smart" Wipe**:
    * If a collection exists, it uses `mongoTemplate.remove(new Query())`. This clears all documents while **preserving the Indexes** (e.g., BIC, Account, and Timestamp indexes) created via Mongosh.
3.  **Generation**:
    * **Baseline**: Generates 5 "Normal" transactions per day for 30 days during business hours (09:00 - 17:00).
    * **Anomalies**: Injects three specific fraud patterns into today's data:
        * **Night Shift**: High-value payment at **03:15 AM** via `MOBILE` channel.
        * **Offshore Switch**: Payment to a **Cayman Islands** BIC where `isModified` is true.
        * **Account Probing**: A $1.25 payment that was `REJECTED` with code `AC04`.

---

## ⚙️ Configuration

### Enable/Disable Seeding
To refresh the database on start, set this to `true`. To stop the refresh and keep your current data, set to `false`.

```properties
# application.properties
app.db.seed-data=true
```

### Manual Database Reset
To wipe the entire database (including collections and indexes) via the Compass terminal:

```javascript
use fraud_detection_db;
db.dropDatabase();
// Then re-run the Mongosh setup script provided in the documentation
```

---

## 🛠️ Troubleshooting

* **Indexes Missing**: If you find that indexes are missing after a refresh, it means the collection was dropped instead of cleared. Ensure the `remove(new Query())` method is used in the `PaymentDataLoader` and not `drop()`.
* **Issue: Application attempts to connect to localhost:27017**: This project uses a programmatic `MongoConfig.java` to override Spring Boot milestone defaults. Ensure your `atlas_uri` in that class is correct.
* **Timestamp Formatting**: The system uses `Instant` for `eventTimestamp`. In MongoDB Compass, these will appear as native **Date** objects, allowing for effortless date-range filtering.