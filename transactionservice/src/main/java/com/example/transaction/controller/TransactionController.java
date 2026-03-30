
package com.example.transaction.controller;

import com.example.transaction.dto.*;
import com.example.transaction.model.Transaction;
import com.example.transaction.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController{

 private final TransactionService service;

 public TransactionController(TransactionService service){
  this.service=service;
 }

 @PostMapping("/deposit")
 public Transaction deposit(@RequestBody DepositRequest r){
  return service.deposit(r);
 }

 @PostMapping("/withdraw")
 public Transaction withdraw(@RequestBody WithdrawRequest r){
  return service.withdraw(r);
 }

 @PostMapping("/transfer")
 public Transaction transfer(@RequestBody TransferRequest r){
  return service.transfer(r);
 }

 @GetMapping("/history/{accountId}")
 public List<Transaction> getHistory(@PathVariable Long accountId){
  return service.getHistory(accountId);
 }
}

