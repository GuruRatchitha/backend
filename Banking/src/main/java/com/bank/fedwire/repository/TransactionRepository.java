package com.bank.fedwire.repository;

import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("select count(t) from BankTransaction t where t.transactionStatus = :#{#status.name()}")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("""
            select count(c)
            from BankTransaction c
            where c.transactionStatus = :#{#status.name()}
            and c.account.user.userId = :userId
            """)
    long countByStatusAndUserId(@Param("status") TransactionStatus status, @Param("userId") Long userId);

    @EntityGraph(attributePaths = "account")
    List<Transaction> findTop5ByAccountUserUserIdOrderByTransactionDateTimeDesc(Long userId);

    @EntityGraph(attributePaths = "account")
    List<Transaction> findByAccountUserUserIdOrderByTransactionDateTimeDesc(Long userId);

    @EntityGraph(attributePaths = "account")
    Page<Transaction> findByAccountUserUserIdOrderByTransactionDateTimeDesc(Long userId, Pageable pageable);
}
