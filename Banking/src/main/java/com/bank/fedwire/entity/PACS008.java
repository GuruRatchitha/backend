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

import java.math.BigDecimal;

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
    private Long pacs008Id;

    private String instructionId;

    private String endToEndId;

    private String transactionId;

    private String debtorAccount;

    private String creditorAccount;

    private BigDecimal amount;

    private String currency;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String xmlPayload;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "messageId")
    private MessageHeader messageHeader;

    @Column(name = "bank_transaction_id")
    private Long bankTransactionId;
}
