package com.bank.fedwire.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class BusinessMessageSequenceService {

    private static final String SEQUENCE_SQL = """
            INSERT INTO business_message_sequence (sequence_date, sequence_value, created_at, updated_at)
            VALUES (?, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE
                sequence_value = sequence_value + 1,
                updated_at = CURRENT_TIMESTAMP(6)
            """;

    private static final String SELECT_SQL = """
            SELECT sequence_value
            FROM business_message_sequence
            WHERE sequence_date = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextBusinessMessageId() {
        LocalDate businessDate = LocalDate.now(ZoneOffset.UTC);
        int sequence = incrementSequence(businessDate);
        return buildBusinessMessageId(businessDate, sequence);
    }

    String buildBusinessMessageId(LocalDate businessDate, int sequence) {
        return businessDate.toString().replace("-", "") + "N1N2G3H4" + String.format("%06d", sequence);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int nextSequenceValue(LocalDate businessDate) {
        return incrementSequence(businessDate);
    }

    private int incrementSequence(LocalDate businessDate) {
        if (businessDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "businessDate is required");
        }

        jdbcTemplate.update(SEQUENCE_SQL, java.sql.Date.valueOf(businessDate));
        Integer value = jdbcTemplate.queryForObject(SELECT_SQL, Integer.class, java.sql.Date.valueOf(businessDate));
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate business message sequence");
        }
        if (value > 999999) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Business message sequence exhausted for " + businessDate);
        }
        return value;
    }
}
