package com.bank.fedwire.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

    private String originalMessageId;

    private String transactionStatus;

    private String reasonCode;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String xmlPayload;

    @Column(name = "transaction_id")
    private Long transactionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private MessageHeader messageHeader;
}
