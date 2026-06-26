package com.bank.fedwire.repository;

import com.bank.fedwire.entity.PACS002;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PACS002Repository extends JpaRepository<PACS002, Long> {

    Optional<PACS002> findByMessageId(String messageId);

    Optional<PACS002> findByOriginalMessageId(String originalMessageId);

    Optional<PACS002> findByTransferId(String transferId);

    Optional<PACS002> findTopByTransactionIdOrderByReceivedTimestampDesc(Long transactionId);

    boolean existsByMessageId(String messageId);

    boolean existsByOriginalMessageId(String originalMessageId);

    boolean existsByTransferId(String transferId);
}
