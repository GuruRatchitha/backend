package com.bank.fedwire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private Long accountId;

    private Long userId;

    private String accountNumber;

    private String iban;

    private String routingNumber;

    private BigDecimal balance;

    private String accountType;

    private String currency;

    private String status;

    private LocalDateTime createdDate;
}
