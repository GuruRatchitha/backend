package com.bank.fedwire.repository;

import com.bank.fedwire.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    boolean existsByUserIdAndAccountNumber(Long userId, String accountNumber);

    List<Beneficiary> findByUserIdOrderByCreatedDateDesc(Long userId);

    List<Beneficiary> findByStatusOrderByCreatedDateDesc(String status);

    Optional<Beneficiary> findByUserIdAndAccountNumberAndRoutingNumber(Long userId, String accountNumber, String routingNumber);
}
