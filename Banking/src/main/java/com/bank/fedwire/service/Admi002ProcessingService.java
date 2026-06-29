package com.bank.fedwire.service;

import com.bank.fedwire.dto.Admi002MessageDto;
import com.bank.fedwire.entity.ADMI002;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.ADMI002Repository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Admi002ProcessingService {

    private final ADMI002Repository admi002Repository;
    private final PACS008Repository pacs008Repository;
    private final TransactionRepository transactionRepository;
    private final Admi002XmlParserService admi002XmlParserService;

    @Transactional
    public void process(String xmlPayload) {
        Admi002MessageDto parsed = admi002XmlParserService.parse(xmlPayload);

        if (isAlreadyProcessed(parsed)) {
            log.info("Skipping duplicate ADMI002 messageType={}, businessMessageId={}, originalReference={}, processingResult=SKIPPED_DUPLICATE",
                    parsed.getMessageType(), parsed.getBusinessMessageId(), parsed.getOriginalReference());
            return;
        }

        Optional<Transaction> transaction = resolveTransaction(parsed.getOriginalReference());
        Long transactionId = transaction.map(Transaction::getTransactionId).orElse(null);

        ADMI002 entity = ADMI002.builder()
                .originalReference(parsed.getOriginalReference())
                .businessMessageId(parsed.getBusinessMessageId())
                .rejectReasonCode(parsed.getRejectReasonCode())
                .rejectReasonDescription(parsed.getRejectReasonDescription())
                .rejectionDateTime(parsed.getRejectionDateTime())
                .xmlPayload(parsed.getXmlPayload())
                .transactionId(transactionId)
                .receivedTimestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        admi002Repository.save(entity);

        log.info("Processed ADMI002 messageType={}, businessMessageId={}, originalReference={}, transactionId={}, processingResult={}",
                parsed.getMessageType(), parsed.getBusinessMessageId(), parsed.getOriginalReference(),
                transactionId, transactionId != null ? "STORED_WITH_TRANSACTION" : "STORED_WITHOUT_TRANSACTION");
    }

    private boolean isAlreadyProcessed(Admi002MessageDto parsed) {
        if (parsed.getBusinessMessageId() != null && admi002Repository.existsByBusinessMessageId(parsed.getBusinessMessageId())) {
            return true;
        }

        return parsed.getOriginalReference() != null
                && admi002Repository.existsByOriginalReference(parsed.getOriginalReference());
    }

    private Optional<Transaction> resolveTransaction(String originalReference) {
        if (originalReference == null || originalReference.isBlank()) {
            return Optional.empty();
        }

        String value = originalReference.trim();
        Optional<PACS008> pacs008 = pacs008Repository.findByMessageId(value);
        if (pacs008.isEmpty()) {
            pacs008 = pacs008Repository.findTopByTransferIdOrderByCreatedDateDesc(value);
        }

        return pacs008.flatMap(record -> transactionRepository.findById(record.getTransactionId()));
    }
}
