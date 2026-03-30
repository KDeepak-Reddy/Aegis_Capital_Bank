package com.account.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accno;

    @Column(nullable = false)
    private String ifsccode;

    @Column(nullable = false)
    private String bankname;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String pin;

    @Version
    private Long version;
}
