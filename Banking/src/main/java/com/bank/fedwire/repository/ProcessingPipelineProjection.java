package com.bank.fedwire.repository;

import java.time.LocalDateTime;

public interface ProcessingPipelineProjection {

    Long getTransactionId();

    String getTransactionStatus();

    LocalDateTime getTransactionDateTime();

    Long getPacs008Id();

    String getPacs008XmlPayload();

    LocalDateTime getPacs008CreatedDate();

    LocalDateTime getPacs008SentAt();

    String getPacs008MessageId();

    Long getPacs002Id();

    String getPacs002TransactionStatus();

    LocalDateTime getPacs002ReceivedTimestamp();

    Long getAdmi002Id();

    LocalDateTime getAdmi002ReceivedTimestamp();

    LocalDateTime getBeneficiarySettlementAt();

    LocalDateTime getReturnSettlementAt();
}
