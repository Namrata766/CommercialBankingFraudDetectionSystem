# 🏦 Commercial Banking Fraud Detection System

This project uses **MongoDB Atlas** to store and analyze payment transactions across three major banking rails. The system includes an automated data seeder designed to simulate 30 days of transaction history plus specific anomaly cases for testing the AI and Rule Engine.

---

## Database Setup

## 📊 Collection Architecture

The database `fraud_detection_db` consists of three primary collections. Each collection represents a specific payment rail with unique risk profiles:

| Collection Name | Rail Type | Typical Data Volume (POC) | Description |
| :--- | :--- | :--- | :--- |
| `wire_payments` | **WIRE** | 151 Records | High-value, real-time gross settlements (RTGS). |
| `ach_payments` | **ACH** | 151 Records | Batch-processed low-value clearing house transfers. |
| `instant_payments` | **INSTANT** | 151 Records | Real-time retail/corporate payments (e.g., RTP, FedNow). |

### Data Schema (`PaymentDocument`)
Each document contains the following core fields:
* **`paymentId`**: Unique identifier (e.g., `PMT-WIRE-20260328-1`).
* **`amount` / `currency`**: Transaction value (standardized to USD).
* **`debtor` / `creditor`**: Nested objects containing `name` and `accountId`.
* **`executionDate`**: ISO String for time-series analysis.
* **`status`**: Current state of the payment (e.g., `COMPLETED`, `PENDING`).
* **`flags`**:
    * `isSTP`: Straight-Through Processing status.
    * `isSanctionHit`: Boolean flag for AML/Watchlist matching.
    * `isModified`: Indicates if the payment was altered after submission.

---

## 🚀 Seeding & Data Refresh Logic

The `PaymentDataLoader` is a `CommandLineRunner` that executes on application startup. It follows a **"Smart Refresh"** strategy to ensure your environment stays clean without losing performance optimizations.

### How it works:
1. **Validation**: The loader checks if the collection exists in your Atlas Cluster.
2. **The "Fresh" Wipe**:
    * If the collection is missing, it is created programmatically.
    * If it exists, it uses `mongoTemplate.remove(new Query())`. This clears all documents (the data) but **preserves the Indexes** (the performance) created via your Mongosh script.
3. **Generation**:
    * **Baseline**: Generates 5 "Normal" transactions per day for the last 30 days to establish a behavior pattern.
    * **Anomaly**: Injects 1 specific "Fraud/Anomaly" case for the current date.

### Anomaly Scenarios Generated:
* **Wire**: A $500,000.00 payment to an `UNKNOWN_OFFSHORE_CORP` with `isModified: true`.
* **Instant**: A payment with `isSanctionHit: true` to trigger the AI's risk reasoning.
* **ACH**: A high-velocity amount ($25,000.00) originating from an unusual channel.

---

## ⚙️ Configuration

To control the data seeding behavior, use the following properties in your `application.properties` or `application.yml`.

### Enable/Disable Seeding
To refresh the database on the next restart, ensure this flag is `true`. To preserve your data and stop the refresh cycle, set it to `false`.

```properties
# Toggle Data Seeding
app.db.seed-data=true
```

### Manual Database Reset
If you need to manually wipe the database and recreate the indexes from scratch (using the Mongosh script in Compass), run this in your Compass terminal:
```
use fraud_detection_db;
db.dropDatabase();
// Then re-run the Mongosh setup script provided in the documentation
```
## 🛠️ Troubleshooting
* **Indexes Missing**: If you find that indexes are missing after a refresh, it means the collection was dropped instead of cleared. Ensure the `remove(new Query())` method is used in the `PaymentDataLoader` and not `drop()`.
* **Issue: Application attempts to connect to localhost:27017**: Fix: This project uses a programmatic MongoConfig.java to override Spring Boot milestone defaults. Ensure your `atlas_uri` in that class is correct.
