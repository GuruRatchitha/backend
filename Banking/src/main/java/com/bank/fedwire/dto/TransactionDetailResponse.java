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
public class TransactionDetailResponse {

    private Long transactionId;

    private String transactionReference;

    private BigDecimal amount;

    private String currency;

    private String purpose;

    private String transactionStatus;

    private String transferStatus;

    private LocalDateTime createdDate;

    private String accountNumber;

    private String accountType;

    private String beneficiaryName;

    private String beneficiaryAccountNumber;

    private String beneficiaryRoutingNumber;

    private String beneficiaryTownName;

    private String beneficiaryCountryCode;
}
