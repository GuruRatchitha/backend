package com.bank.fedwire.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessMessageSequenceServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BusinessMessageSequenceService businessMessageSequenceService;

    @Test
    void nextSequenceValueReturnsPersistedValue() {
        when(jdbcTemplate.update(anyString(), any(java.sql.Date.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(java.sql.Date.class))).thenReturn(1);

        assertEquals(1, businessMessageSequenceService.nextSequenceValue(LocalDate.of(2026, 6, 26)));
    }

    @Test
    void buildBusinessMessageIdUsesFixedFormat() {
        assertEquals("20260626N1N2G3H4000001",
                businessMessageSequenceService.buildBusinessMessageId(LocalDate.of(2026, 6, 26), 1));
    }
}
