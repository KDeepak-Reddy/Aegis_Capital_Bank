package com.example.transaction.service;

import com.example.transaction.dto.*;
import com.example.transaction.model.Transaction;
import com.example.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService{

 private final TransactionRepository repository;
 private final RestTemplate restTemplate;

 @Value("${account-service.url:http://localhost:5050}")
 private String accountServiceBaseUrl;

 public TransactionService(TransactionRepository repository, RestTemplate restTemplate){
  this.repository=repository;
  this.restTemplate=restTemplate;
 }

 private boolean verifyPin(Long accountId, String pin) {
  try {
   ResponseEntity<Boolean> response = restTemplate.postForEntity(
    accountServiceBaseUrl + "/internal/accounts" + "/" + accountId + "/verify-pin?pin=" + pin,
    null,
    Boolean.class
   );
   return response.getStatusCode() == HttpStatus.OK && Boolean.TRUE.equals(response.getBody());
  } catch (Exception e) {
   return false;
  }
 }

 @Transactional
 public Transaction deposit(DepositRequest req){
  Transaction tx=new Transaction();
  tx.setAccountId(req.getAccountId());
  tx.setAmount(req.getAmount());
  tx.setType("DEPOSIT");

  if (!verifyPin(req.getAccountId(), req.getPin())) {
   tx.setStatus("FAILED: INVALID PIN");
   return repository.save(tx);
  }

  // Update balance in Account Service
  try {
   restTemplate.put(
     accountServiceBaseUrl + "/internal/accounts" + "/" + req.getAccountId() + "/balance?amount=" + req.getAmount() + "&type=DEPOSIT",
     null
   );
   tx.setStatus("SUCCESS");
  } catch (Exception e) {
   tx.setStatus("FAILED: " + extractErrorMessage(e));
  }

  return repository.save(tx);
 }

 @Transactional
 public Transaction withdraw(WithdrawRequest req){
  Transaction tx=new Transaction();
  tx.setAccountId(req.getAccountId());
  tx.setAmount(req.getAmount());
  tx.setType("WITHDRAW");

  if (!verifyPin(req.getAccountId(), req.getPin())) {
   tx.setStatus("FAILED: INVALID PIN");
   return repository.save(tx);
  }

  // Update balance in Account Service
  try {
   restTemplate.put(
     accountServiceBaseUrl + "/internal/accounts" + "/" + req.getAccountId() + "/balance?amount=" + req.getAmount() + "&type=WITHDRAW",
     null
   );
   tx.setStatus("SUCCESS");
  } catch (Exception e) {
   tx.setStatus("FAILED: " + extractErrorMessage(e));
  }

  return repository.save(tx);
 }

 @Transactional
 public Transaction transfer(TransferRequest req){
  Transaction tx=new Transaction();
  tx.setAccountId(req.getFromAccountId());
  tx.setReferenceAccountId(req.getToAccountId());
  tx.setAmount(req.getAmount());
  tx.setType("TRANSFER");

  if (!verifyPin(req.getFromAccountId(), req.getPin())) {
   tx.setStatus("FAILED: INVALID PIN");
   return repository.save(tx);
  }

  // Step 1: Withdraw from source account
  try {
   restTemplate.put(
     accountServiceBaseUrl + "/internal/accounts" + "/" + req.getFromAccountId() + "/balance?amount=" + req.getAmount() + "&type=WITHDRAW",
     null
   );
  } catch (Exception e) {
   tx.setStatus("FAILED: " + extractErrorMessage(e));
   return repository.save(tx);
  }

  // Step 2: Deposit to destination account
  try {
   restTemplate.put(
     accountServiceBaseUrl + "/internal/accounts" + "/" + req.getToAccountId() + "/balance?amount=" + req.getAmount() + "&type=DEPOSIT",
     null
   );
   tx.setStatus("SUCCESS");
  } catch (Exception e) {
   // Saga compensating action: reverse the withdraw from source
   try {
    restTemplate.put(
      accountServiceBaseUrl + "/internal/accounts" + "/" + req.getFromAccountId() + "/balance?amount=" + req.getAmount() + "&type=DEPOSIT",
      null
    );
    tx.setStatus("FAILED: Destination account error. Amount refunded to source.");
   } catch (Exception rollbackEx) {
    // Critical: rollback itself failed — requires manual intervention
    tx.setStatus("FAILED: CRITICAL - Withdraw succeeded but deposit and rollback both failed. Manual review needed.");
   }
  }

  return repository.save(tx);
 }

 public List<Transaction> getHistory(Long accountId){
  return repository.findByAccountId(accountId);
 }

 /**
  * Extracts a user-friendly error message from exceptions thrown by RestTemplate
  */
 private String extractErrorMessage(Exception e) {
  String msg = e.getMessage();
  if (msg != null && msg.contains("Insufficient balance")) {
   return "Insufficient balance";
  }
  if (msg != null && msg.length() > 100) {
   return msg.substring(0, 100);
  }
  return msg != null ? msg : "Unknown error";
 }
}
