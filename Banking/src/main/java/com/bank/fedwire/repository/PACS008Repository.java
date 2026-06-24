package com.bank.fedwire.repository;

import com.bank.fedwire.entity.PACS008;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PACS008Repository extends JpaRepository<PACS008, Long> {

    Optional<PACS008> findByTransactionId(Long transactionId);
}
