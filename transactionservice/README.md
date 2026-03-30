# 💸 Transaction Service

Transaction processing microservice for Aegis Capital Bank — handles deposits, withdrawals, transfers (own & external accounts), and transaction history.

## Tech Stack

| Layer     | Technology                      |
|-----------|---------------------------------|
| Framework | Spring Boot 3                   |
| Language  | Java 17+                        |
| Database  | MySQL (`transaction_Service`)   |
| Build     | Maven                           |
| Frontend  | Vanilla HTML / CSS / JS         |

## Project Structure

```
transactionservice/
├── src/main/java/com/example/transaction/
│   ├── controller/
│   │   └── TransactionController.java   # REST endpoints
│   ├── dto/
│   │   ├── DepositRequest.java          # Deposit request DTO
│   │   ├── WithdrawRequest.java         # Withdraw request DTO
│   │   └── TransferRequest.java         # Transfer request DTO
│   ├── model/
│   │   └── Transaction.java             # Transaction entity
│   ├── repository/
│   │   └── TransactionRepository.java   # Spring Data JPA repository
│   ├── config/
│   │   └── AppConfig.java               # RestTemplate & CORS beans
│   └── service/
│       └── TransactionService.java      # Business logic + PIN verification
├── src/main/resources/
│   └── application.yml                  # Port 5005, DB config
├── Frontend/
│   ├── index.html                       # Transaction dashboard UI
│   ├── app.js                           # Frontend logic (deposit, withdraw, transfer, history)
│   ├── index.css                        # Styles
│   └── package.json
├── pom.xml
└── README.md
```

## API Endpoints

| Method | Path                               | Description                                  |
|--------|-------------------------------------|----------------------------------------------|
| POST   | `/transactions/deposit`             | Deposit funds (requires PIN)                 |
| POST   | `/transactions/withdraw`            | Withdraw funds (requires PIN)                |
| POST   | `/transactions/transfer`            | Transfer between accounts (requires PIN)     |
| GET    | `/transactions/history/{accountId}` | Get transaction history for an account       |

### Request Bodies

**Deposit / Withdraw:**
```json
{
  "accountId": 1,
  "amount": 500.0,
  "pin": "1234"
}
```

**Transfer:**
```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 500.0,
  "pin": "1234"
}
```

### Transaction Statuses

| Status               | Meaning                              |
|----------------------|--------------------------------------|
| `SUCCESS`            | Transaction completed successfully   |
| `FAILED`             | General failure (e.g., server error) |
| `FAILED: INVALID PIN`| Incorrect PIN was provided          |

## Running Locally

### Prerequisites
- Java 17+
- Maven
- MySQL running on `localhost:3306`
- Account Service running on `localhost:5050` (required for PIN verification & balance updates)

### Steps

```bash
# 1. Start the backend (port 5005)
mvn spring-boot:run

# 2. Start the frontend dev server
cd Frontend
npm install
npm start
```

The database `transaction_Service` is created automatically on first run.

## Inter-Service Communication

- **Calls** the Account Service's internal endpoints to:
  - Verify transaction PINs → `POST /internal/accounts/{id}/verify-pin`
  - Update balances on deposit/withdraw/transfer → `PUT /internal/accounts/{id}/balance`
- The Account Service must be running for transactions to succeed.
