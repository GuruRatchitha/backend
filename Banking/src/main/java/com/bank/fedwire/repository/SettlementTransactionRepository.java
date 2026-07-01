package com.bank.fedwire.repository;

import com.bank.fedwire.entity.SettlementTransaction;
import com.bank.fedwire.entity.SettlementTransactionStatus;
import com.bank.fedwire.entity.SettlementTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    @Query("""
            select coalesce(sum(st.amount), 0)
            from SettlementTransaction st
            where st.transactionType = :transactionType
            """)
    BigDecimal sumAmountByTransactionType(@Param("transactionType") SettlementTransactionType transactionType);

    default BigDecimal sumAmountWaitingToRevert() {
        return sumAmountWaitingToRevert(
                SettlementTransactionType.DEBIT_TO_SETTLEMENT,
                SettlementTransactionType.RETURN_TO_SENDER,
                SettlementTransactionStatus.SUCCESS,
                "RETURN");
    }

    @Query("""
            select coalesce(sum(st.amount), 0)
            from SettlementTransaction st
            where st.transactionType = :debitType
              and exists (
                  select 1
                  from BankTransaction t
                  where t.transactionId = st.paymentId
                    and upper(t.transactionStatus) = :returnStatus
              )
              and not exists (
                  select 1
                  from SettlementTransaction returned
                  where returned.paymentId = st.paymentId
                    and returned.transactionType = :returnType
                    and returned.status = :successStatus
              )
            """)
    BigDecimal sumAmountWaitingToRevert(
            @Param("debitType") SettlementTransactionType debitType,
            @Param("returnType") SettlementTransactionType returnType,
            @Param("successStatus") SettlementTransactionStatus successStatus,
            @Param("returnStatus") String returnStatus);
}
