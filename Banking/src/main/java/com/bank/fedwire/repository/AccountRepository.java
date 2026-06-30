package com.bank.fedwire.repository;

import com.bank.fedwire.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserUserId(Long userId);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNameAndUserIsNull(String accountName);

    Optional<Account> findByAccountType(String accountType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountName = :accountName and a.user is null")
    Optional<Account> findByAccountNameForUpdate(@Param("accountName") String accountName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountType = :accountType")
    Optional<Account> findByAccountTypeForUpdate(@Param("accountType") String accountType);

    long countByUserUserId(Long userId);

    @Query("select coalesce(sum(a.balance), 0) from Account a")
    BigDecimal sumTotalBalance();

    @Query("""
            select upper(a.accountType), count(a)
            from Account a
            group by upper(a.accountType)
            """)
    List<Object[]> countAccountsByType();

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByIban(String iban);

    boolean existsByRoutingNumber(String routingNumber);
}
