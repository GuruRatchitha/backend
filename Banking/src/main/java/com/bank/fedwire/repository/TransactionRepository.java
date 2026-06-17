package com.bank.fedwire.repository;

import com.bank.fedwire.entity.CreditTransfer;
import com.bank.fedwire.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<CreditTransfer, Long> {

    @Query("select count(c) from CreditTransfer c where c.transferStatus = :#{#status.name()}")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("""
            select count(c)
            from CreditTransfer c
            where c.transferStatus = :#{#status.name()}
            and c.account.user.userId = :userId
            """)
    long countByStatusAndUserId(@Param("status") TransactionStatus status, @Param("userId") Long userId);
}
