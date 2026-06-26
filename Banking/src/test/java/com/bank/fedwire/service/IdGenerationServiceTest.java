package com.bank.fedwire.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdGenerationServiceTest {

    @Mock
    private BusinessMessageSequenceService businessMessageSequenceService;

    @InjectMocks
    private IdGenerationService idGenerationService;

    @Test
    void generateMessageIdMatchesBusinessMessageId() {
        when(businessMessageSequenceService.nextBusinessMessageId())
                .thenReturn("20260626N1N2G3H4000001");

        assertEquals("20260626N1N2G3H4000001", idGenerationService.generateBusinessMessageId());
        assertEquals("20260626N1N2G3H4000001", idGenerationService.generateMessageId());
    }
}
