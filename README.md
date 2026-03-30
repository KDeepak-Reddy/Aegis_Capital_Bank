# Aegis Capital Bank - Microservices Architecture

This is a microservices-based banking system comprising three independent services with a localized frontend layer. The stack is containerized using Docker and Docker Compose.

## Architecture

- **Auth Service** `(:5052 / :5173)`: Handles user registration, login with TOTP MFA, password recovery, and JWT generation.
- **Account Service** `(:5050 / :5501)`: Manages banking accounts, balance checks, and PIN verification. Exposes secure API endpoints (requires JWT) and internal endpoints for inter-service communication.
- **Transaction Service** `(:5005 / :5502)`: Processes deposits, withdrawals, and transfers by communicating internally with the Account Service.

All data is stored in isolated MySQL databases running within a single MySQL Docker container on port `3306` (mapped to host `3307`).

## 🚀 Running the Application

Ensure you have Docker and Docker Compose installed. From the root directory, run:

```bash
docker-compose up --build -d
```

### Accessing the Frontends:
1. **Authentication:** [http://localhost:5173](http://localhost:5173) (Register & Login)
2. **Account Dashboard:** [http://localhost:5501](http://localhost:5501) (Manage Accounts & PIN)
3. **Transactions:** [http://localhost:5502](http://localhost:5502) (Deposit, Withdraw, Transfer)

## 🛠️ Recent Improvements & Bug Fixes

### 1. Dashboard Routing & Quick Access
- **Replaced "Transactions" Card with "My Profile":** The Auth Dashboard's "Transactions" card was causing an "Access Denied" error because transactions require an explicit `accountId`. It was replaced with a **"My Profile"** card that correctly opens the user profile dropdown. Users now navigate to Transactions via the **My Accounts** page.

### 2. Reset PIN Persistence (Backend + Frontend)
- **Frontend Form Bug:** The `app.js` script tag in the `account-frontend` was loading *before* the Reset PIN modal HTML was parsed. This caused the Javascript event listener (`addEventListener('submit')`) to drop. As a result, the form submitted naturally, refreshing the page and wiping out the API call and success message. **Fix:** Added `defer` to the script tag and moved it below the modal HTML.
- **Backend JPA Versioning Bug:** The `AccountService` `resetPin()` method was originally using `accountRepository.save(account)`. Due to Hibernate's `@Version` optimistic locking, updating just the PIN wasn't always triggering a dirty check correctly, causing the update to be silently ignored. **Fix:** Replaced the `save()` call with a direct `@Modifying(clearAutomatically = true, flushAutomatically = true) @Query("UPDATE Account a SET a.pin = :newPin WHERE ...")` to bypass JPA caching and enforce the DB update.

### 3. PasswordEncoder Dependency Injection
- Created a Spring-managed `@Bean PasswordEncoder` inside `AppConfig.java` and injected it into `AccountService.java` to replace the local `new BCryptPasswordEncoder()` instantiation, ensuring consistent hashing across the application lifecycle.

### 4. Container Optimization
- Ensured all Spring Boot backend configurations (like database URLs, server ports, and CORS) accurately reflect the internal Docker network aliases (e.g., `mysql`, `account-backend:5050`).
