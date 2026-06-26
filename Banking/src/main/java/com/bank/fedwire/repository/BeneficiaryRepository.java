package com.bank.fedwire.repository;

import com.bank.fedwire.entity.Beneficiary;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
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

    long countByStatusIgnoreCase(String status);

    @Query("""
            select new com.bank.fedwire.dto.PendingBeneficiaryResponse(
                    b.beneficiaryId,
                    u.userName,
                    b.beneficiaryName,
                    b.bankName,
                    b.accountNumber,
                    b.status,
                    b.createdDate
            )
            from Beneficiary b
            left join b.user u
            where upper(b.status) = upper(:status)
            order by b.createdDate desc
            """)
    List<com.bank.fedwire.dto.PendingBeneficiaryResponse> findPendingBeneficiaries(
            @Param("status") String status,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Beneficiary b where b.beneficiaryId = :beneficiaryId")
    Optional<Beneficiary> findByIdForPaymentUpdate(@Param("beneficiaryId") Long beneficiaryId);
}
