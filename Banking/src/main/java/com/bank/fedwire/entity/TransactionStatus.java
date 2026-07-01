package com.bank.fedwire.entity;

public enum TransactionStatus {
    APPROVED,
    PROCESSING,
    ON_HOLD,
    REJECTED,
    RETURN,
    REVERTED,
    COMPLETED,
    FAILED,
    PENDING,
    WAITING_FOR_PACS002,
    SUCCESS,
    ADMI002_RECEIVED,
    INVALID_MESSAGE
}
