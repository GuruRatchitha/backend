package com.bank.fedwire.repository;

import com.bank.fedwire.entity.MessageHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageHeaderRepository extends JpaRepository<MessageHeader, String> {
}
