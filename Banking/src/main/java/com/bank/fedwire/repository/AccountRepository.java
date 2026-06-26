package com.bank.fedwire.repository;

import com.bank.fedwire.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserUserId(Long userId);

    Optional<Account> findByAccountNumber(String accountNumber);

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
}
