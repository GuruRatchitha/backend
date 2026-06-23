package com.bank.fedwire.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IdGenerationService {

    private static final char[] UPPER_ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AtomicInteger sequence = new AtomicInteger(0);

    public String generateBusinessMessageId() {
        return generateMessageId();
    }

    public String generateMessageId() {
        return LocalDate.now().format(BASIC_DATE) + randomUpperAlphanumeric(8) + nextSixDigitSequence();
    }

    public String generateUniqueAlphanumericHyphenId(String prefix) {
        return prefix + "-" + LocalDate.now().format(BASIC_DATE) + randomUpperAlphanumeric(8) + nextSixDigitSequence();
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
        return generateMessageId();
    }

    public String generateInstructionId() {
        return generateUniqueAlphanumericHyphenId("INS");
    }

    public String generateTxId() {
        return generateUniqueAlphanumericHyphenId("TX");
    }

    public String generateMemberId() {
        return String.format("%09d", nextSequenceValue());
    }

    private String randomUpperAlphanumeric(int length) {
        StringBuilder value = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            value.append(UPPER_ALPHANUMERIC[RANDOM.nextInt(UPPER_ALPHANUMERIC.length)]);
        }
        return value.toString();
    }

    private String nextSixDigitSequence() {
        return String.format("%06d", nextSequenceValue());
    }

    private int nextSequenceValue() {
        return sequence.updateAndGet(current -> current >= 999999 ? 1 : current + 1);
    }
}
