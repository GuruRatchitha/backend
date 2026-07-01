package com.bank.fedwire.repository;

import com.bank.fedwire.entity.SettlementTransaction;
import com.bank.fedwire.entity.SettlementTransactionStatus;
import com.bank.fedwire.entity.SettlementTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementTransactionRepository extends JpaRepository<SettlementTransaction, Long>,
        JpaSpecificationExecutor<SettlementTransaction> {

    List<SettlementTransaction> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    List<SettlementTransaction> findByStatusOrderByCreatedAtDesc(SettlementTransactionStatus status);

    List<SettlementTransaction> findByTransactionTypeOrderByCreatedAtDesc(SettlementTransactionType transactionType);

    Optional<SettlementTransaction> findTopByPaymentIdAndTransactionTypeOrderByCreatedAtDesc(
            Long paymentId, SettlementTransactionType transactionType);

    boolean existsByPaymentIdAndTransactionTypeAndStatus(
            Long paymentId, SettlementTransactionType transactionType, SettlementTransactionStatus status);
}
