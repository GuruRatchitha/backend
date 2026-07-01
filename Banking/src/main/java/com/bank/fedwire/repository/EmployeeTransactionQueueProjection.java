package com.bank.fedwire.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EmployeeTransactionQueueProjection {

    Long getTransactionId();

    String getTransactionReference();

    String getSenderName();

    String getBeneficiaryName();

    BigDecimal getAmount();

    String getStatus();

    LocalDateTime getPaymentDate();
}
