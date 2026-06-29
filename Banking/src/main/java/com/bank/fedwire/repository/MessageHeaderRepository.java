package com.bank.fedwire.repository;

import com.bank.fedwire.entity.MessageHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageHeaderRepository extends JpaRepository<MessageHeader, String> {

    Optional<MessageHeader> findByTransactionId(Long transactionId);

    Optional<MessageHeader> findTopByTransactionIdOrderByCreatedDateDesc(Long transactionId);
}
