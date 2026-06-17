package com.bank.fedwire.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credit_transfer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transferId;

    private String transactionReference;

    private BigDecimal amount;

    private String currency;

    private String purpose;

    private String transferStatus;

    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accountId")
    @JsonBackReference("account-credit-transfers")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiaryId")
    @JsonBackReference("beneficiary-credit-transfers")
    private Beneficiary beneficiary;

    @OneToOne(mappedBy = "creditTransfer", fetch = FetchType.LAZY)
    @JsonManagedReference("credit-transfer-pacs008")
    private PACS008 pacs008;

    @OneToOne(mappedBy = "creditTransfer", fetch = FetchType.LAZY)
    @JsonManagedReference("credit-transfer-pacs002")
    private PACS002 pacs002;

    @OneToOne(mappedBy = "creditTransfer", fetch = FetchType.LAZY)
    @JsonManagedReference("credit-transfer-adm002")
    private ADM002 adm002;

    @OneToMany(mappedBy = "creditTransfer")
    @JsonManagedReference("credit-transfer-message-headers")
    @Builder.Default
    private List<MessageHeader> messageHeaders = new ArrayList<>();
}
