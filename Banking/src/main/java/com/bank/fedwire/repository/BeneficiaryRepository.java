package com.bank.fedwire.repository;

import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.BeneficiaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, BeneficiaryId> {

    boolean existsByUserIdAndAccountNumber(Long userId, String accountNumber);

    List<Beneficiary> findByUserIdOrderByCreatedDateDesc(Long userId);
}
