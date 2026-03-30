package com.account.service;

import com.account.entity.Account;
import com.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Account> getAccountsByUserEmail(String userEmail) {
        return accountRepository.findByUserEmail(userEmail);
    }

    public Optional<Account> getAccountByIdAndEmail(Long id, String userEmail) {
        return accountRepository.findById(id)
                .filter(account -> account.getUserEmail().equals(userEmail));
    }

    @Transactional
    public Account createAccount(String userEmail, String bankName, String ifscCode, BigDecimal initialBalance, String pin) {
        Account account = Account.builder()
                .accno(generateAccountNumber())
                .bankname(bankName)
                .ifsccode(ifscCode)
                .balance(initialBalance != null ? initialBalance : BigDecimal.ZERO)
                .userEmail(userEmail)
                .pin(passwordEncoder.encode(pin))
                .build();
        return accountRepository.save(account);
    }

    public BigDecimal getBalance(Long id, String userEmail) {
        return getAccountByIdAndEmail(id, userEmail)
                .map(Account::getBalance)
                .orElseThrow(() -> new RuntimeException("Account not found or access denied"));
    }

    @Transactional
    public Account updateBalance(Long id, BigDecimal amount, String type) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        if ("DEPOSIT".equalsIgnoreCase(type)) {
            account.setBalance(account.getBalance().add(amount));
        } else if ("WITHDRAW".equalsIgnoreCase(type)) {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance");
            }
            account.setBalance(account.getBalance().subtract(amount));
        } else {
            throw new RuntimeException("Invalid type: " + type + ". Must be DEPOSIT or WITHDRAW.");
        }

        return accountRepository.save(account);
    }

    public Optional<Account> findByAccno(String accno) {
        return accountRepository.findByAccno(accno);
    }

    public boolean verifyPin(Long id, String pin) {
        return accountRepository.findById(id)
                .map(Account::getPin)
                .filter(hashedPin -> passwordEncoder.matches(pin, hashedPin))
                .isPresent();
    }

    @Transactional
    public void resetPin(Long id, String userEmail, String oldPin, String newPin) {
        // 1. Load account to verify ownership + old PIN
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (!account.getUserEmail().equals(userEmail)) {
            throw new RuntimeException("Access denied");
        }
        if (!passwordEncoder.matches(oldPin, account.getPin())) {
            throw new RuntimeException("Current PIN is incorrect");
        }
        // 2. Directly update PIN in DB via JPQL — bypasses @Version / entity caching
        String encoded = passwordEncoder.encode(newPin);
        int updated = accountRepository.updatePin(id, userEmail, encoded);
        if (updated == 0) {
            throw new RuntimeException("Failed to update PIN. Please try again.");
        }
    }

    private String generateAccountNumber() {
        return String.valueOf(System.currentTimeMillis()).substring(2) + (int)(Math.random() * 1000);
    }
}
