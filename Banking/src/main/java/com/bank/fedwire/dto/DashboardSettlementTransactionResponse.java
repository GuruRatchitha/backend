package com.bank.fedwire.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DashboardSettlementTransactionResponse(
        Long paymentId,
        String accountNumber,
        BigDecimal amount,
        String status,
        LocalDateTime createdDate
) {
}
