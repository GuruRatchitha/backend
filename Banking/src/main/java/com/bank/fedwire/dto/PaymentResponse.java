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
public class PaymentResponse {

    private Long transactionId;

    private String transferId;

    private String paymentTransactionId;

    private String bankTransactionId;

    private String messageId;

    private String businessMessageId;

    private Long pacs008Id;

    private String instructionId;

    private String txId;

    private String uetr;

    private String endToEndId;

    private String senderAccountNumber;

    private String senderAccountType;

    private BigDecimal amount;

    private String currency;

    private String transactionStatus;

    private LocalDateTime createdDate;
}
