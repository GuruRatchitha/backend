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
        return LocalDate.now().format(BASIC_DATE) + randomUpperAlphanumeric(8) + nextSixDigitSequence();
    }

    public String generateSampleUuid35() {
        return UUID.randomUUID().toString().substring(0, 35);
    }

    public String generateUetr() {
        return UUID.randomUUID().toString();
    }

    public String generateEndToEndId() {
        return "INV" + String.format("%07d", nextSequenceValue());
    }

    public String generateTransferId() {
        return generateSampleUuid35();
    }

    public String generatePaymentTransactionId() {
        return generateSampleUuid35();
    }

    public String generateBankTransactionId() {
        return generateBusinessMessageId();
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
