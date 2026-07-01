package com.bank.fedwire.service;

import com.bank.fedwire.dto.Admi002MessageDto;
import com.bank.fedwire.entity.ADMI002;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.SettlementTransactionStatus;
import com.bank.fedwire.entity.SettlementTransactionType;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.entity.TransactionStatus;
import com.bank.fedwire.repository.ADMI002Repository;
import com.bank.fedwire.repository.MessageHeaderRepository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.SettlementTransactionRepository;
import com.bank.fedwire.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Admi002ProcessingService {

    private final ADMI002Repository admi002Repository;
    private final PACS008Repository pacs008Repository;
    private final TransactionRepository transactionRepository;
    private final MessageHeaderRepository messageHeaderRepository;
    private final SettlementTransactionRepository settlementTransactionRepository;
    private final Admi002ParserService admi002ParserService;

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void process(String xmlPayload) {
        Admi002MessageDto parsed;
        try {
            log.info("Parsing ADMI002 XML payload length={}", xmlPayload != null ? xmlPayload.length() : 0);
            parsed = admi002ParserService.parse(xmlPayload);
        } catch (ResponseStatusException ex) {
            log.error("Malformed ADMI002 XML; preserving raw XML before retry, xmlPayload={}", xmlPayload, ex);
            admi002Repository.saveAndFlush(ADMI002.builder()
                    .errorDescription("Malformed ADMI002 XML")
                    .xmlPayload(xmlPayload)
                    .receivedTimestamp(LocalDateTime.now(ZoneOffset.UTC))
                    .build());
            throw ex;
        }

        if (isAlreadyProcessed(parsed)) {
            log.info("Duplicate ignored ADMI002 messageType={}, messageId={}, businessMessageId={}, originalMessageId={}, relatedMessageId={}, processingResult=SKIPPED_DUPLICATE",
                    parsed.getMessageType(), parsed.getMessageId(), parsed.getBusinessMessageId(),
                    parsed.getOriginalMessageId(), parsed.getRelatedMessageId());
            return;
        }

        Optional<Transaction> transaction = resolveTransaction(parsed);
        Long transactionId = transaction.map(Transaction::getTransactionId).orElse(null);
        transaction.ifPresentOrElse(
                value -> log.info("Transaction resolved for ADMI002 transactionId={}, messageId={}, originalMessageId={}, businessMessageId={}, relatedMessageId={}",
                        value.getTransactionId(), parsed.getMessageId(), parsed.getOriginalMessageId(),
                        parsed.getBusinessMessageId(), parsed.getRelatedMessageId()),
                () -> log.warn("ADMI002 transaction could not be resolved messageId={}, originalMessageId={}, businessMessageId={}, relatedMessageId={}",
                        parsed.getMessageId(), parsed.getOriginalMessageId(), parsed.getBusinessMessageId(),
                        parsed.getRelatedMessageId()));

        ADMI002 entity = ADMI002.builder()
                .messageId(parsed.getMessageId())
                .originalMessageId(parsed.getOriginalMessageId())
                .originalReference(parsed.getOriginalReference())
                .businessMessageId(parsed.getBusinessMessageId())
                .relatedMessageId(parsed.getRelatedMessageId())
                .errorCode(parsed.getErrorCode())
                .errorDescription(parsed.getErrorDescription())
                .severity(parsed.getSeverity())
                .creationDateTime(parsed.getCreationDateTime())
                .rejectReasonCode(parsed.getErrorCode())
                .rejectReasonDescription(parsed.getErrorDescription())
                .rejectionDateTime(parsed.getRejectionDateTime())
                .xmlPayload(parsed.getXmlPayload())
                .transactionId(transactionId)
                .receivedTimestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        admi002Repository.saveAndFlush(entity);
        log.info("Database updated for ADMI002 admi002Id={}, transactionId={}, messageId={}, errorCode={}",
                entity.getAdmi002Id(), entity.getTransactionId(), entity.getMessageId(), entity.getErrorCode());
        transaction.ifPresent(this::markTransactionAdmi002Received);

        log.info("Processing completed for ADMI002 messageType={}, messageId={}, businessMessageId={}, originalMessageId={}, relatedMessageId={}, transactionId={}, processingResult={}",
                parsed.getMessageType(), parsed.getMessageId(), parsed.getBusinessMessageId(),
                parsed.getOriginalMessageId(), parsed.getRelatedMessageId(),
                transactionId, transactionId != null ? "STORED_WITH_TRANSACTION" : "STORED_WITHOUT_TRANSACTION");
    }

    private boolean isAlreadyProcessed(Admi002MessageDto parsed) {
        if (parsed.getMessageId() != null && admi002Repository.existsByMessageId(parsed.getMessageId())) {
            return true;
        }
        return false;
    }

    private Optional<Transaction> resolveTransaction(Admi002MessageDto parsed) {
        List<String> candidates = List.of(
                nullToBlank(parsed.getOriginalMessageId()),
                nullToBlank(parsed.getBusinessMessageId()),
                nullToBlank(parsed.getRelatedMessageId()),
                nullToBlank(parsed.getOriginalReference()),
                nullToBlank(parsed.getMessageId()));

        for (String candidate : candidates) {
            if (candidate.isBlank()) {
                continue;
            }

            Optional<Transaction> transaction = resolveTransactionByReference(candidate.trim());
            if (transaction.isPresent()) {
                return transaction;
            }
        }

        return Optional.empty();
    }

    private Optional<Transaction> resolveTransactionByReference(String reference) {
        Optional<Transaction> byMessageHeader = messageHeaderRepository.findByMessageId(reference)
                .or(() -> messageHeaderRepository.findByBusinessMessageId(reference))
                .map(MessageHeader::getTransactionId)
                .flatMap(transactionRepository::findById);
        if (byMessageHeader.isPresent()) {
            return byMessageHeader;
        }

        Optional<PACS008> pacs008 = pacs008Repository.findByMessageId(reference)
                .or(() -> pacs008Repository.findTopByTransferIdOrderByCreatedDateDesc(reference))
                .or(() -> pacs008Repository.findByTransferId(reference))
                .or(() -> pacs008Repository.findByTxId(reference))
                .or(() -> pacs008Repository.findByInstructionId(reference))
                .or(() -> pacs008Repository.findByEndToEndId(reference));

        return pacs008.flatMap(record -> transactionRepository.findById(record.getTransactionId()));
    }

    private void markTransactionAdmi002Received(Transaction transaction) {
        Transaction lockedTransaction = transactionRepository.findByTransactionIdForUpdate(transaction.getTransactionId())
                .orElse(transaction);
        String status = settlementTransactionRepository.existsByPaymentIdAndTransactionTypeAndStatus(
                lockedTransaction.getTransactionId(),
                SettlementTransactionType.DEBIT_TO_SETTLEMENT,
                SettlementTransactionStatus.SUCCESS)
                ? TransactionStatus.RETURN.name()
                : TransactionStatus.FAILED.name();
        lockedTransaction.setTransactionStatus(status);
        lockedTransaction.setPendingPaymentKey(null);
        transactionRepository.saveAndFlush(lockedTransaction);

        messageHeaderRepository.findTopByTransactionIdOrderByCreatedDateDesc(lockedTransaction.getTransactionId())
                .ifPresent(messageHeader -> {
                    messageHeader.setMessageStatus(status);
                    messageHeaderRepository.saveAndFlush(messageHeader);
                });
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
