# 🔐 Auth Service

Authentication & Authorization microservice for Aegis Capital Bank — handles user registration, login, JWT issuance, and TOTP-based Multi-Factor Authentication (MFA).

## Tech Stack

| Layer     | Technology              |
|-----------|-------------------------|
| Framework | Spring Boot 3           |
| Language  | Java 17+                |
| Database  | MySQL (`auth_Service`)  |
| Security  | Spring Security + JWT (HS256) |
| MFA       | TOTP (Time-based One-Time Password) |
| Build     | Maven                   |
| Frontend  | Vanilla HTML / CSS / JS |

## Project Structure

```
auth_service/
├── Backend/
│   ├── src/main/java/com/example/auth/
│   │   ├── controller/
│   │   │   ├── AuthController.java      # Register, Login, MFA endpoints
│   │   │   └── AppController.java       # Misc / profile endpoints
│   │   ├── dto/                         # Request & Response DTOs
│   │   ├── model/
│   │   │   ├── User.java               # User entity (name, email, PAN, MFA secret, etc.)
│   │   │   └── Role.java               # Role entity (RBAC)
│   │   ├── repository/                  # Spring Data JPA repositories
│   │   ├── security/
│   │   │   ├── JwtService.java          # JWT generation & validation
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── SecurityConfig.java      # Spring Security config
│   │   │   └── CustomUserDetailsService.java
│   │   └── service/
│   │       ├── AuthService.java         # Registration & login logic
│   │       ├── MfaService.java          # TOTP generation & verification
│   │       └── UserService.java         # User lookup helpers
│   └── src/main/resources/
│       └── application.yml              # Port, DB, JWT config
├── Frontend/
│   ├── index.html                       # Landing page
│   ├── register.html                    # Registration form
│   ├── login.html                       # Login form
│   ├── mfa.html                         # MFA verification page
│   ├── dashboard.html                   # Post-login dashboard
│   └── index.css                        # Styles
└── README.md
```

## API Endpoints

| Method | Path               | Description                      | Auth |
|--------|--------------------|----------------------------------|------|
| POST   | `/auth/register`   | Register a new user              | ✗    |
| POST   | `/auth/login`      | Login (returns JWT or MFA token) | ✗    |
| POST   | `/auth/verify-mfa` | Verify TOTP code after login     | ✗    |

## Running Locally

### Prerequisites
- Java 17+
- Maven
- MySQL running on `localhost:3306`

### Steps

```bash
# 1. Start the backend (default port: 8080)
cd Backend
mvn spring-boot:run

# 2. Start the frontend dev server
cd Frontend
npm install
npm start
```

The database `auth_Service` is created automatically on first run (`createDatabaseIfNotExist=true`).

## Inter-Service Communication

- Issues **JWT tokens** consumed by the Account Service for authenticated requests.
- The JWT secret must match across Auth and Account services.
