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
public class EmployeeTransactionQueueResponse {

    private Long transactionId;

    private String transactionReference;

    private String senderName;

    private String beneficiaryName;

    private BigDecimal amount;

    private String status;

    private LocalDateTime paymentDate;
}
