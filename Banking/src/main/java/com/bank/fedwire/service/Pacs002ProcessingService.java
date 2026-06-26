package com.bank.fedwire.service;

import com.bank.fedwire.dto.Pacs002MessageDto;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.PACS002;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.MessageHeaderRepository;
import com.bank.fedwire.repository.PACS002Repository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class Pacs002ProcessingService {

    private final PACS002Repository pacs002Repository;
    private final PACS008Repository pacs008Repository;
    private final TransactionRepository transactionRepository;
    private final MessageHeaderRepository messageHeaderRepository;
    private final Pacs002XmlParserService pacs002XmlParserService;

    @Transactional
    public void process(String xmlPayload) {
        Pacs002MessageDto parsed = pacs002XmlParserService.parse(xmlPayload);

        if (isAlreadyProcessed(parsed)) {
            log.info("Skipping duplicate PACS002 message messageId={}, originalMessageId={}, transferId={}",
                    parsed.getMessageId(), parsed.getOriginalMessageId(), parsed.getTransferId());
            return;
        }

        Transaction transaction = resolveTransaction(parsed);

        String normalizedStatus = normalizeStatus(parsed.getTransactionStatus());
        if (normalizedStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PACS002 transaction_status is required");
        }

        transaction.setTransactionStatus(normalizedStatus);
        transactionRepository.save(transaction);

        MessageHeader messageHeader = messageHeaderRepository.findByTransactionId(transaction.getTransactionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Message header not found for transactionId " + transaction.getTransactionId()));
        messageHeader.setMessageStatus(normalizedStatus);
        messageHeaderRepository.save(messageHeader);

        PACS002 pacs002 = PACS002.builder()
                .originalMessageId(parsed.getOriginalMessageId())
                .messageId(parsed.getMessageId())
                .transferId(parsed.getTransferId())
                .transactionStatus(normalizedStatus)
                .reasonCode(parsed.getReasonCode())
                .xmlPayload(parsed.getXmlPayload())
                .transactionId(transaction.getTransactionId())
                .receivedTimestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        pacs002Repository.save(pacs002);

        log.info("Processed PACS002 for transactionId={}, transferId={}, originalMessageId={}, status={}",
                transaction.getTransactionId(), parsed.getTransferId(), parsed.getOriginalMessageId(), normalizedStatus);
    }

    private boolean isAlreadyProcessed(Pacs002MessageDto parsed) {
        if (parsed.getMessageId() != null && pacs002Repository.existsByMessageId(parsed.getMessageId())) {
            return true;
        }

        if (parsed.getOriginalMessageId() != null && pacs002Repository.existsByOriginalMessageId(parsed.getOriginalMessageId())) {
            return true;
        }

        return parsed.getTransferId() != null && pacs002Repository.existsByTransferId(parsed.getTransferId());
    }

    private Transaction resolveTransaction(Pacs002MessageDto parsed) {
        if (parsed.getTransferId() != null && !parsed.getTransferId().isBlank()) {
            Transaction transaction = transactionRepository.findByTransferId(parsed.getTransferId().trim())
                    .orElse(null);
            if (transaction != null) {
                return transaction;
            }
        }

        if (parsed.getOriginalMessageId() != null && !parsed.getOriginalMessageId().isBlank()) {
            return pacs008Repository.findByMessageId(parsed.getOriginalMessageId().trim())
                    .map(pacs008 -> transactionRepository.findById(pacs008.getTransactionId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Transaction not found for originalMessageId " + parsed.getOriginalMessageId())))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "PACS008 record not found for originalMessageId " + parsed.getOriginalMessageId()));
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "PACS002 must include transfer_id or original_message_id");
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String value = status.trim().toUpperCase(Locale.ROOT);
        if (value.contains("RJCT") || "REJECTED".equals(value)) {
            return "FAILED";
        }
        if (value.contains("ACCP") || value.contains("ACSC") || value.contains("ACTC")
                || "APPROVED".equals(value) || "COMPLETED".equals(value)) {
            return "COMPLETED";
        }
        if (value.contains("PDNG") || value.contains("ACSP") || "PROCESSING".equals(value)) {
            return "PROCESSING";
        }
        return value;
    }
}
