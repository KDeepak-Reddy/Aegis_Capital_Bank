//package com.account.controller;
//
//import com.account.entity.Account;
//import com.account.service.AccountService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/accounts")
//@RequiredArgsConstructor
//public class AccountController {
//
//    private final AccountService accountService;
//
//    @GetMapping
//    public ResponseEntity<List<Account>> getUserAccounts(Authentication authentication) {
//        String email = authentication.getName();
//        return ResponseEntity.ok(accountService.getAccountsByUserEmail(email));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<Account> getAccountById(@PathVariable Long id, Authentication authentication) {
//        String email = authentication.getName();
//        return accountService.getAccountByIdAndEmail(id, email)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    @GetMapping("/{id}/balance")
//    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable Long id, Authentication authentication) {
//        String email = authentication.getName();
//        try {
//            return ResponseEntity.ok(accountService.getBalance(id, email));
//        } catch (Exception e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @PostMapping
//    public ResponseEntity<Account> createAccount(
//            @RequestParam String bankName,
//            @RequestParam String ifscCode,
//            @RequestParam(required = false) BigDecimal initialBalance,
//            Authentication authentication) {
//
//        String email = authentication.getName();
//        Account newAccount = accountService.createAccount(email, bankName, ifscCode, initialBalance);
//        return ResponseEntity.ok(newAccount);
//    }
//}


package com.account.controller;

import com.account.entity.Account;
import com.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    private String currentUserEmailOrThrow(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Unauthenticated");
        }
        return authentication.getName();
    }

    // ─── Authenticated endpoints (JWT required) ─────────────────────
    @GetMapping("/api/accounts")
    public ResponseEntity<List<Account>> getUserAccounts(Authentication authentication) {
        String email = currentUserEmailOrThrow(authentication);
        return ResponseEntity.ok(accountService.getAccountsByUserEmail(email));
    }

    @GetMapping("/api/accounts/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id, Authentication authentication) {
        String email = currentUserEmailOrThrow(authentication);
        return accountService.getAccountByIdAndEmail(id, email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/accounts/{id}/balance")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable Long id, Authentication authentication) {
        String email = currentUserEmailOrThrow(authentication);
        return accountService.getAccountByIdAndEmail(id, email)
                .map(Account::getBalance)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Accepts URL query params as your frontend sends
    @PostMapping("/api/accounts")
    public ResponseEntity<Account> createAccount(
            @RequestParam String bankName,
            @RequestParam String ifscCode,
            @RequestParam(required = false) BigDecimal initialBalance,
            @RequestParam String pin,
            Authentication authentication) {

        String email = currentUserEmailOrThrow(authentication);

        Account newAccount = accountService.createAccount(email, bankName, ifscCode, initialBalance, pin);
        // Return 201 Created with Location
        return ResponseEntity
                .created(URI.create("/api/accounts/" + newAccount.getId()))
                .body(newAccount);
    }

    // ─── Internal endpoints (no JWT required) ───────────────────────
    @PutMapping("/internal/accounts/{id}/balance")
    public ResponseEntity<Account> updateBalance(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam String type) {
        try {
            Account updated = accountService.updateBalance(id, amount, type);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/internal/accounts/by-accno/{accno}")
    public ResponseEntity<Account> getAccountByAccno(@PathVariable String accno) {
        return accountService.findByAccno(accno)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/internal/accounts/{id}/verify-pin")
    public ResponseEntity<Boolean> verifyPin(@PathVariable Long id, @RequestParam String pin) {
        boolean isValid = accountService.verifyPin(id, pin);
        if (isValid) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false);
        }
    }

    @PutMapping("/api/accounts/{id}/reset-pin")
    public ResponseEntity<String> resetPin(
            @PathVariable Long id,
            @RequestParam String oldPin,
            @RequestParam String newPin,
            Authentication authentication) {
        String email = currentUserEmailOrThrow(authentication);
        try {
            accountService.resetPin(id, email, oldPin, newPin);
            return ResponseEntity.ok("PIN updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

