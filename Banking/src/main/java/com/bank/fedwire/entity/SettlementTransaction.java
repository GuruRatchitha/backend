package com.bank.fedwire.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "settlement_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_transaction_id")
    private Long settlementTransactionId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "sender_account", nullable = false, length = 255)
    private String senderAccount;

    // TEMPORARY FOR PAYAPT ADM.002 TESTING
    @Column(name = "beneficiary_account", nullable = false, length = 1024)
    // END TEMPORARY
    private String beneficiaryAccount;

    @Column(name = "settlement_account", nullable = false, length = 255)
    private String settlementAccount;

    // TEMPORARY FOR PAYAPT ADM.002 TESTING
    @Column(nullable = false, precision = 65, scale = 2)
    // END TEMPORARY
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private SettlementTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SettlementTransactionStatus status;

    @Column(name = "pacs008_message_id", length = 22)
    private String pacs008MessageId;

    @Column(name = "pacs002_status", length = 16)
    private String pacs002Status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
