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
public class TransactionResponse {

    private String transactionReference;

    private String beneficiaryName;

    private BigDecimal amount;

    private String currency;

    private String purpose;

    private String transactionStatus;

    private String transferStatus;

    private LocalDateTime transactionDate;

    private String accountNumber;

    private String accountType;
}
