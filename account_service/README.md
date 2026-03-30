# 🏦 Account Service

Account management microservice for Aegis Capital Bank — handles bank account creation, balance enquiry, and exposes internal endpoints consumed by the Transaction Service.

## Tech Stack

| Layer     | Technology                  |
|-----------|-----------------------------|
| Framework | Spring Boot 3               |
| Language  | Java 17+                    |
| Database  | MySQL (`account_Service`)   |
| Security  | Spring Security + JWT (HS256) |
| Build     | Maven                       |
| Frontend  | Vanilla HTML / CSS / JS     |

## Project Structure

```
account_service/
├── Backend/
│   ├── src/main/java/com/account/
│   │   ├── controller/
│   │   │   └── AccountController.java   # Authenticated + Internal endpoints
│   │   ├── entity/
│   │   │   └── Account.java             # Account entity (accno, balance, PIN, etc.)
│   │   ├── repository/
│   │   │   └── AccountRepository.java   # Spring Data JPA repository
│   │   ├── security/
│   │   │   ├── JwtAuthFilter.java       # JWT authentication filter
│   │   │   ├── JwtUtil.java             # JWT token utilities
│   │   │   └── SecurityConfig.java      # Security rules (public vs protected routes)
│   │   ├── config/
│   │   │   └── CorsConfig.java          # CORS configuration
│   │   └── service/
│   │       └── AccountService.java      # Business logic
│   └── src/main/resources/
│       └── application.yml              # Port 5050, DB, JWT config
├── Frontend/
│   ├── index.html                       # Account dashboard
│   ├── app.js                           # Frontend logic
│   ├── index.css                        # Styles
│   └── package.json
└── README.md
```

## API Endpoints

### Authenticated Endpoints (JWT Required)

| Method | Path                          | Description                       |
|--------|-------------------------------|-----------------------------------|
| GET    | `/api/accounts`               | List all accounts for the user    |
| GET    | `/api/accounts/{id}`          | Get account by ID                 |
| GET    | `/api/accounts/{id}/balance`  | Get account balance               |
| POST   | `/api/accounts`               | Create a new account              |

### Internal Endpoints (No JWT — consumed by Transaction Service)

| Method | Path                                      | Description                           |
|--------|-------------------------------------------|---------------------------------------|
| PUT    | `/internal/accounts/{id}/balance`         | Update balance (deposit/withdraw)     |
| GET    | `/internal/accounts/by-accno/{accno}`     | Lookup account by account number      |
| POST   | `/internal/accounts/{id}/verify-pin`      | Verify transaction PIN                |

## Running Locally

### Prerequisites
- Java 17+
- Maven
- MySQL running on `localhost:3306`

### Steps

```bash
# 1. Start the backend (port 5050)
cd Backend
mvn spring-boot:run

# 2. Start the frontend dev server
cd Frontend
npm install
npm start
```

The database `account_Service` is created automatically on first run.

## Inter-Service Communication

- **Consumes** JWT tokens issued by the Auth Service for protected `/api/*` endpoints.
- **Exposes** internal `/internal/*` endpoints for the Transaction Service to:
  - Verify transaction PINs
  - Update account balances
  - Lookup accounts by account number
