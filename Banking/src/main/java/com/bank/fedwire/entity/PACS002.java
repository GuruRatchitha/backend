package com.bank.fedwire.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pacs002")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PACS002 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pacs002Id;

    @Column(name = "original_message_id")
    private String originalMessageId;

    @Column(name = "message_id", length = 64)
    private String messageId;

    @Column(name = "transfer_id", length = 35)
    private String transferId;

    @Column(name = "transaction_status")
    private String transactionStatus;

    @Column(name = "reason_code")
    private String reasonCode;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String xmlPayload;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "received_timestamp")
    private LocalDateTime receivedTimestamp;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            referencedColumnName = "message_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(value = jakarta.persistence.ConstraintMode.NO_CONSTRAINT)
    )
    private MessageHeader messageHeader;
}
