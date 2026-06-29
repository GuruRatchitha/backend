package com.bank.fedwire.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class IdGenerationService {

    private static final char[] UPPER_ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AtomicInteger sequence = new AtomicInteger(0);
    private final BusinessMessageSequenceService businessMessageSequenceService;

    public String generateBusinessMessageId() {
        return businessMessageSequenceService.nextBusinessMessageId();
    }

    public String generateMessageId() {
        return generateBusinessMessageId();
    }

    public String generateUniqueAlphanumericHyphenId(String prefix) {
        LocalDate businessDate = LocalDate.now(ZoneOffset.UTC);
        return prefix + "-" + businessDate.format(BASIC_DATE)
                + randomUpperAlphanumeric(8) + nextLegacySixDigitSequence();
    }

    public String generateUetr() {
        return UUID.randomUUID().toString();
    }

    public String generateEndToEndId() {
        return "INV0019253";
    }

    public String generateTransferId() {
        return generateUniqueAlphanumericHyphenId("TRF");
    }

    public String generatePaymentTransactionId() {
        return generateUniqueAlphanumericHyphenId("PMT");
    }

    public String generateBankTransactionId() {
        return generateLegacyMessageId();
    }

    public String generateInstructionId() {
        return generateUniqueAlphanumericHyphenId("INS");
    }

    public String generateTxId() {
        return generateUniqueAlphanumericHyphenId("TX");
    }

    private String generateLegacyMessageId() {
        LocalDate businessDate = LocalDate.now(ZoneOffset.UTC);
        return businessDate.format(BASIC_DATE) + randomUpperAlphanumeric(8) + nextLegacySixDigitSequence();
    }

    private String randomUpperAlphanumeric(int length) {
        StringBuilder value = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            value.append(UPPER_ALPHANUMERIC[RANDOM.nextInt(UPPER_ALPHANUMERIC.length)]);
        }
        return value.toString();
    }

    private String nextLegacySixDigitSequence() {
        return String.format("%06d", nextLegacySequenceValue());
    }

    private int nextLegacySequenceValue() {
        return sequence.updateAndGet(current -> current >= 999999 ? 1 : current + 1);
    }
}
