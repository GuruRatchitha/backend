package com.bank.fedwire.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementTransactionResponse(
        Long settlementTransactionId,
        Long paymentId,
        String accountNumber,
        String senderAccountNumber,
        String senderName,
        String senderAccountType,
        String beneficiaryAccountNumber,
        String receiverAccountNumber,
        String receiverName,
        String receiverAccountType,
        String senderAccount,
        String beneficiaryAccount,
        String settlementAccount,
        BigDecimal amount,
        String status,
        String pacs008MessageId,
        String uetr,
        String pacs002Status,
        LocalDateTime dateTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LedgerPartyDisplay senderDisplay,
        LedgerPartyDisplay receiverDisplay
) {
    public record LedgerPartyDisplay(
            String accountNumber,
            String name,
            String accountType
    ) {
    }
}
