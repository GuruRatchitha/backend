package com.bank.fedwire.repository;

import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.entity.TransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByPendingPaymentKey(String pendingPaymentKey);

    @EntityGraph(attributePaths = {"account", "account.user"})
    @Query("select distinct t from BankTransaction t left join fetch t.account a left join fetch a.user where t.transactionId = :transactionId")
    Optional<Transaction> findByTransactionId(@Param("transactionId") Long transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"account", "account.user"})
    @Query("select t from BankTransaction t where t.transactionId = :transactionId")
    Optional<Transaction> findByTransactionIdForUpdate(@Param("transactionId") Long transactionId);

    @EntityGraph(attributePaths = {"account", "account.user"})
    @Query("select distinct t from BankTransaction t left join fetch t.account a left join fetch a.user where t.transferId = :transferId")
    Optional<Transaction> findByTransferId(@Param("transferId") String transferId);

    @Query("select count(t) from BankTransaction t where t.transactionStatus = :#{#status.name()}")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("select count(t) from BankTransaction t where upper(t.transactionStatus) = upper(:status)")
    long countByTransactionStatusIgnoreCase(@Param("status") String status);

    @Query("""
            select count(c)
            from BankTransaction c
            where c.transactionStatus = :#{#status.name()}
            and c.account.user.userId = :userId
            """)
    long countByStatusAndUserId(@Param("status") TransactionStatus status, @Param("userId") Long userId);

    @Query("""
            select count(c)
            from BankTransaction c
            where c.transactionStatus in :statuses
            and c.account.user.userId = :userId
            """)
    long countByTransactionStatusInAndUserId(@Param("statuses") Collection<String> statuses, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"account", "account.user"})
    List<Transaction> findTop5ByAccountUserUserIdOrderByTransactionDateTimeDesc(Long userId);

    @EntityGraph(attributePaths = {"account", "account.user"})
    List<Transaction> findByAccountUserUserIdOrderByTransactionDateTimeDesc(Long userId);

    @Query("""
            select new com.bank.fedwire.dto.PendingTransactionResponse(
                    t.transactionId,
                    u.userName,
                    t.amount,
                    t.transactionType,
                    t.transactionStatus,
                    t.transactionDateTime
            )
            from BankTransaction t
            left join t.account a
            left join a.user u
            where upper(t.transactionStatus) = upper(:status)
            order by t.transactionDateTime desc
            """)
    List<com.bank.fedwire.dto.PendingTransactionResponse> findPendingTransactions(
            @Param("status") String status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"account", "account.user"})
    Page<Transaction> findByAccountUserUserIdOrderByTransactionDateTimeDesc(Long userId, Pageable pageable);

    @Query("""
            select new com.bank.fedwire.dto.EmployeeTransactionQueueResponse(
                    t.transactionId,
                    t.transferId,
                    u.userName,
                    t.beneficiaryName,
                    t.amount,
                    t.transactionStatus,
                    t.transactionDateTime
            )
            from BankTransaction t
            left join t.account a
            left join a.user u
            order by t.transactionDateTime desc
            """)
    List<com.bank.fedwire.dto.EmployeeTransactionQueueResponse> findEmployeeTransactionQueue();
}
