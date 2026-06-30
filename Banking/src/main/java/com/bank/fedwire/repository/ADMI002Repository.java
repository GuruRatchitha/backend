package com.bank.fedwire.repository;

import com.bank.fedwire.entity.ADMI002;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ADMI002Repository extends JpaRepository<ADMI002, Long> {

    Optional<ADMI002> findByMessageId(String messageId);

    Optional<ADMI002> findByBusinessMessageId(String businessMessageId);

    Optional<ADMI002> findByOriginalReference(String originalReference);

    Optional<ADMI002> findTopByTransactionIdOrderByReceivedTimestampDesc(Long transactionId);

    boolean existsByMessageId(String messageId);

    boolean existsByBusinessMessageId(String businessMessageId);

    boolean existsByOriginalReference(String originalReference);
}
