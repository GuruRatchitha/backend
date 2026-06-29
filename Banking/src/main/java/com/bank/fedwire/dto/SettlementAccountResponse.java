package com.bank.fedwire.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementAccountResponse(
        Long accountId,
        String accountNumber,
        String accountName,
        String accountType,
        String currency,
        BigDecimal balance,
        String status,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
) {
}
