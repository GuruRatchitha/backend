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
public class PendingTransactionResponse {

    private Long transactionId;

    private String customerName;

    private BigDecimal amount;

    private String transactionType;

    private String status;

    private LocalDateTime createdDate;
}
