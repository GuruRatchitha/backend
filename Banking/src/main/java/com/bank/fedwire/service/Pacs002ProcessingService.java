package com.bank.fedwire.service;

import com.bank.fedwire.dto.Pacs002MessageDto;
import com.bank.fedwire.entity.PACS002;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.PACS002Repository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Pacs002ProcessingService {

    private final PACS002Repository pacs002Repository;
    private final PACS008Repository pacs008Repository;
    private final TransactionRepository transactionRepository;
    private final Pacs002XmlParserService pacs002XmlParserService;
    private final SettlementTransactionService settlementTransactionService;

    @Transactional
    public void process(String xmlPayload) {
        try {
            log.info("Parsing PACS002 XML payload length={}", xmlPayload != null ? xmlPayload.length() : 0);
            Pacs002MessageDto parsed = pacs002XmlParserService.parse(xmlPayload);
            log.info("Parsed PACS002 fields original_message_id={}, message_id={}, transfer_id={}, transaction_status={}, reason_code={}",
                    parsed.getOriginalMessageId(), parsed.getMessageId(), parsed.getTransferId(),
                    parsed.getTransactionStatus(), parsed.getReasonCode());

            if (isAlreadyProcessed(parsed)) {
                log.info("Skipping duplicate PACS002 message messageType=pacs.002.001.10, messageId={}, originalReference={}, transactionId={}, processingResult=SKIPPED_DUPLICATE",
                        parsed.getMessageId(), parsed.getOriginalMessageId(), null);
                return;
            }

            String normalizedStatus = normalizeStatus(parsed.getTransactionStatus());
            log.info("Resolving transaction for PACS002 using transferId={}, originalMessageId={}, messageId={}",
                    parsed.getTransferId(), parsed.getOriginalMessageId(), parsed.getMessageId());
            Optional<Transaction> transaction = resolveTransaction(parsed);
            transaction.ifPresentOrElse(
                    value -> log.info("Resolved PACS002 to transactionId={}", value.getTransactionId()),
                    () -> log.warn("PACS002 transaction could not be resolved; saving XML without settlement update messageId={}, originalReference={}, transferId={}",
                            parsed.getMessageId(), parsed.getOriginalMessageId(), parsed.getTransferId()));

            PACS002 pacs002 = PACS002.builder()
                    .originalMessageId(parsed.getOriginalMessageId())
                    .messageId(parsed.getMessageId())
                    .transferId(parsed.getTransferId())
                    .transactionStatus(normalizedStatus)
                    .reasonCode(parsed.getReasonCode())
                    .xmlPayload(parsed.getXmlPayload())
                    .transactionId(transaction.map(Transaction::getTransactionId).orElse(null))
                    .receivedTimestamp(LocalDateTime.now(ZoneOffset.UTC))
                    .build();

            log.info("Persisting PACS002 entity transactionId={}, messageId={}, transferId={}, xmlPayloadLength={}",
                    pacs002.getTransactionId(), pacs002.getMessageId(), pacs002.getTransferId(),
                    pacs002.getXmlPayload() != null ? pacs002.getXmlPayload().length() : 0);
            pacs002Repository.saveAndFlush(pacs002);

            if (transaction.isPresent() && isSettlementStatus(normalizedStatus)) {
                settlementTransactionService.processPacs002(transaction.get(), pacs002);
            } else if (!isSettlementStatus(normalizedStatus)) {
                log.warn("PACS002 XML saved without settlement update because status is missing or unsupported status={}, messageId={}",
                        normalizedStatus, parsed.getMessageId());
            }

            log.info("Processed PACS002 messageType=pacs.002.001.10, messageId={}, originalReference={}, transactionId={}, processingResult=PROCESSED, status={}",
                    parsed.getMessageId(), parsed.getOriginalMessageId(), pacs002.getTransactionId(), normalizedStatus);
        } catch (Exception ex) {
            log.error("PACS002 processing failed", ex);
            throw ex;
        }
    }

    private boolean isAlreadyProcessed(Pacs002MessageDto parsed) {
        if (parsed.getMessageId() != null && pacs002Repository.existsByMessageId(parsed.getMessageId())) {
            log.info("Duplicate PACS002 detected by messageId={}", parsed.getMessageId());
            return true;
        }

        return false;
    }

    private Optional<Transaction> resolveTransaction(Pacs002MessageDto parsed) {
        if (parsed.getTransferId() != null && !parsed.getTransferId().isBlank()) {
            String reference = parsed.getTransferId().trim();
            Optional<Transaction> transaction = transactionRepository.findByTransferId(reference)
                    .or(() -> findTransactionByPacs008Reference(reference));
            if (transaction.isPresent()) {
                return transaction;
            }
        }

        if (parsed.getOriginalMessageId() != null && !parsed.getOriginalMessageId().isBlank()) {
            return pacs008Repository.findByMessageId(parsed.getOriginalMessageId().trim())
                    .flatMap(pacs008 -> transactionRepository.findById(pacs008.getTransactionId()));
        }

        if (parsed.getMessageId() != null && !parsed.getMessageId().isBlank()) {
            return pacs008Repository.findByMessageId(parsed.getMessageId().trim())
                    .flatMap(pacs008 -> transactionRepository.findById(pacs008.getTransactionId()));
        }

        return Optional.empty();
    }

    private Optional<Transaction> findTransactionByPacs008Reference(String reference) {
        return pacs008Repository.findByTransferId(reference)
                .or(() -> pacs008Repository.findByTxId(reference))
                .or(() -> pacs008Repository.findByInstructionId(reference))
                .or(() -> pacs008Repository.findByEndToEndId(reference))
                .flatMap(pacs008 -> transactionRepository.findById(pacs008.getTransactionId()));
    }

    private boolean isSettlementStatus(String status) {
        return "ACSC".equals(status) || "ACCP".equals(status) || "ACSP".equals(status) || "RJCT".equals(status);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String value = status.trim().toUpperCase(Locale.ROOT);
        if (value.contains("RJCT")) {
            return "RJCT";
        }
        if (value.contains("ACSC")) {
            return "ACSC";
        }
        if (value.contains("ACCP")) {
            return "ACCP";
        }
        if (value.contains("ACSP")) {
            return "ACSP";
        }
        return value;
    }
}
