package com.bank.fedwire.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "adm002")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ADMI002 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adm002_id")
    private Long admi002Id;

    @Column(name = "original_reference")
    private String originalReference;

    @Column(name = "business_message_id", length = 22)
    private String businessMessageId;

    @Column(name = "reject_reason_code")
    private String rejectReasonCode;

    @Column(name = "reject_reason_description")
    private String rejectReasonDescription;

    @Column(name = "rejection_date_time")
    private LocalDateTime rejectionDateTime;

    @Lob
    @Column(name = "xml_payload", columnDefinition = "TEXT")
    private String xmlPayload;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "received_timestamp")
    private LocalDateTime receivedTimestamp;
}
