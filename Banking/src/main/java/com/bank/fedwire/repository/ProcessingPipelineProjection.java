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

    String getPacs002ReasonCode();

    LocalDateTime getPacs002ReceivedTimestamp();

    Long getAdmi002Id();

    String getAdmi002RejectReasonDescription();

    String getAdmi002ErrorDescription();

    String getAdmi002RejectReasonCode();

    String getAdmi002ErrorCode();

    LocalDateTime getAdmi002ReceivedTimestamp();

    LocalDateTime getBeneficiarySettlementAt();

    LocalDateTime getReturnSettlementAt();
}
