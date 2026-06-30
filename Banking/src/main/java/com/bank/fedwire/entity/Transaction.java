package com.bank.fedwire.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

@Entity(name = "BankTransaction")
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "transfer_id", nullable = false, unique = true, length = 35)
    private String transferId;

    @Column(name = "payment_transaction_id", nullable = false, unique = true, length = 35)
    private String paymentTransactionId;

    @Column(name = "bank_transaction_id", nullable = false, unique = true, length = 22)
    private String bankTransactionId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    // TEMPORARY FOR PAYAPT ADM.002 TESTING
    @Column(nullable = false, precision = 65, scale = 2)
    // END TEMPORARY
    private BigDecimal amount;

    @Column(name = "beneficiary_name", nullable = false)
    private String beneficiaryName;

    // TEMPORARY FOR PAYAPT ADM.002 TESTING
    @Column(name = "beneficiary_account_number", nullable = false, length = 1024)
    // END TEMPORARY
    private String beneficiaryAccountNumber;

    // TEMPORARY FOR PAYAPT ADM.002 TESTING
    @Column(name = "beneficiary_routing_number", nullable = false, length = 1024)
    // END TEMPORARY
    private String beneficiaryRoutingNumber;

    @Column(name = "pending_payment_key", unique = true, length = 64)
    private String pendingPaymentKey;

    private String remarks;

    @Column(name = "transaction_date_time", nullable = false)
    private LocalDateTime transactionDateTime;

    @Column(name = "transaction_status", nullable = false)
    private String transactionStatus;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "account_number",
            referencedColumnName = "account_number",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    @JsonBackReference("account-transactions")
    private Account account;

    @OneToMany(mappedBy = "transaction")
    @Builder.Default
    private List<MessageHeader> messageHeaders = new ArrayList<>();
}
