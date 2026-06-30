package com.bank.fedwire.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pacs008")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PACS008 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pacs008_id")
    private Long pacs008Id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;

    @Column(name = "message_id", nullable = false, length = 22)
    private String messageId;

    @Column(name = "transfer_id", nullable = false, length = 35)
    private String transferId;

    @Column(name = "instruction_id", nullable = false, length = 35)
    private String instructionId;

    @Column(name = "tx_id", nullable = false, length = 35)
    private String txId;

    @Column(name = "end_to_end_id", nullable = false, length = 10)
    private String endToEndId;

    @Column(nullable = false, length = 36)
    private String uetr;

    @Column(name = "payment_transaction_id", nullable = false, length = 35)
    private String paymentTransactionId;

    @Column(name = "bank_transaction_id", nullable = false, length = 22)
    private String bankTransactionId;

    // TEMPORARY FOR PAYAPT ADM.002 TESTING
    @Column(nullable = false, precision = 65, scale = 2)
    // END TEMPORARY
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "debtor_name", nullable = false, length = 140)
    private String debtorName;

    @Column(name = "debtor_account", nullable = false)
    private String debtorAccount;

    @Column(name = "debtor_town", nullable = false, length = 35)
    private String debtorTown;

    @Column(name = "debtor_country", nullable = false, length = 2)
    private String debtorCountry;

    @Column(name = "creditor_name", nullable = false, length = 140)
    private String creditorName;

    // TEMPORARY FOR PAYAPT ADM.002 TESTING
    @Column(name = "creditor_account", nullable = false, length = 1024)
    // END TEMPORARY
    private String creditorAccount;

    @Column(name = "creditor_town", nullable = false, length = 35)
    private String creditorTown;

    @Column(name = "creditor_country", nullable = false, length = 2)
    private String creditorCountry;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "acceptance_datetime", nullable = false)
    private LocalDateTime acceptanceDatetime;

    @Column(name = "charge_bearer", nullable = false, length = 4)
    private String chargeBearer;

    @Column(name = "local_instrument", nullable = false, length = 4)
    private String localInstrument;

    @Lob
    @Column(name = "xml_payload", columnDefinition = "TEXT")
    private String xmlPayload;

    @Column(name = "sqs_published_at")
    private LocalDateTime sqsPublishedAt;

    @Column(name = "sqs_message_id", length = 128)
    private String sqsMessageId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            referencedColumnName = "message_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private MessageHeader messageHeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "transaction_id",
            referencedColumnName = "transaction_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Transaction transaction;
}
