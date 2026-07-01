package com.bank.fedwire.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementAccountResponse(
        Long accountId,
        String accountNumber,
        String settlementAccountNumber,
        String accountName,
        String accountType,
        String currency,
        BigDecimal balance,
        BigDecimal currentBalance,
        BigDecimal revertAmount,
        BigDecimal revertedAmountBalance,
        String status,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        List<SettlementTransactionResponse> transactionHistory
) {
}
