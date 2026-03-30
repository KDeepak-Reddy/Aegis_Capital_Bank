package com.example.transaction.dto;

import java.math.BigDecimal;

public class DepositRequest{
 private Long accountId;
 private BigDecimal amount;

 public Long getAccountId(){return accountId;}
 public void setAccountId(Long accountId){this.accountId=accountId;}

 public BigDecimal getAmount(){return amount;}
 public void setAmount(BigDecimal amount){this.amount=amount;}

 private String pin;
 public String getPin(){return pin;}
 public void setPin(String pin){this.pin=pin;}
}
