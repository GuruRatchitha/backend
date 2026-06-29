package com.bank.fedwire.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementTransactionResponse(
        Long settlementTransactionId,
        Long paymentId,
        String senderAccount,
        String beneficiaryAccount,
        String settlementAccount,
        BigDecimal amount,
        String transactionType,
        String status,
        String pacs008MessageId,
        String pacs002Status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
