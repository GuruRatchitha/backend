package com.bank.fedwire.repository;

import com.bank.fedwire.entity.Beneficiary;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    boolean existsByUserIdAndAccountNumber(Long userId, String accountNumber);

    List<Beneficiary> findByUserIdOrderByCreatedDateDesc(Long userId);

    List<Beneficiary> findByStatusOrderByCreatedDateDesc(String status);

    Optional<Beneficiary> findByUserIdAndAccountNumberAndRoutingNumber(Long userId, String accountNumber, String routingNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Beneficiary b where b.beneficiaryId = :beneficiaryId")
    Optional<Beneficiary> findByIdForPaymentUpdate(@Param("beneficiaryId") Long beneficiaryId);
}
