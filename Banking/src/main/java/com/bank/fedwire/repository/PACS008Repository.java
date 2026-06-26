package com.bank.fedwire.repository;

import com.bank.fedwire.entity.PACS008;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PACS008Repository extends JpaRepository<PACS008, Long> {

    Optional<PACS008> findByTransactionId(Long transactionId);

    Optional<PACS008> findByTransferId(String transferId);

    Optional<PACS008> findByMessageId(String messageId);

    List<PACS008> findByXmlPayloadIsNotNullAndSqsPublishedAtIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PACS008 p where p.transactionId = :transactionId")
    Optional<PACS008> findByTransactionIdForUpdate(@Param("transactionId") Long transactionId);
}
