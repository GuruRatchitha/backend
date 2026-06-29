package com.bank.fedwire.service;

import com.bank.fedwire.dto.Admi002MessageDto;
import com.bank.fedwire.entity.ADMI002;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.ADMI002Repository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Admi002ProcessingServiceTest {

    @Mock
    private ADMI002Repository admi002Repository;

    @Mock
    private PACS008Repository pacs008Repository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Admi002XmlParserService admi002XmlParserService;

    @InjectMocks
    private Admi002ProcessingService admi002ProcessingService;

    @Test
    void processPersistsResolvedTransaction() {
        String xml = "<Envelope><AppHdr><MsgDefIdr>admi.002.001.01</MsgDefIdr><BizMsgIdr>ADM-1</BizMsgIdr></AppHdr>"
                + "<Document><RltdRef><Ref>TRF-123</Ref></RltdRef>"
                + "<RjctgPtyRsn><Cd>AC04</Cd><RsnDesc>Account closed</RsnDesc></RjctgPtyRsn>"
                + "<RjctnDtTm>2026-06-27T10:15:30</RjctnDtTm></Document></Envelope>";

        Admi002MessageDto parsed = Admi002MessageDto.builder()
                .messageType("admi.002.001.01")
                .businessMessageId("ADM-1")
                .originalReference("TRF-123")
                .rejectReasonCode("AC04")
                .rejectReasonDescription("Account closed")
                .xmlPayload(xml)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(55L)
                .build();

        when(admi002XmlParserService.parse(xml)).thenReturn(parsed);
        when(admi002Repository.existsByBusinessMessageId("ADM-1")).thenReturn(false);
        when(admi002Repository.existsByOriginalReference("TRF-123")).thenReturn(false);
        when(pacs008Repository.findByMessageId("TRF-123")).thenReturn(Optional.of(PACS008.builder()
                .transactionId(55L)
                .build()));
        when(transactionRepository.findById(55L)).thenReturn(Optional.of(transaction));

        admi002ProcessingService.process(xml);

        ArgumentCaptor<ADMI002> captor = ArgumentCaptor.forClass(ADMI002.class);
        verify(admi002Repository).save(captor.capture());

        ADMI002 saved = captor.getValue();
        assertEquals("TRF-123", saved.getOriginalReference());
        assertEquals("ADM-1", saved.getBusinessMessageId());
        assertEquals("AC04", saved.getRejectReasonCode());
        assertEquals("Account closed", saved.getRejectReasonDescription());
        assertEquals(55L, saved.getTransactionId());
    }

    @Test
    void processStoresMessageWhenTransactionCannotBeResolved() {
        String xml = "<Envelope><AppHdr><MsgDefIdr>admi.002.001.01</MsgDefIdr><BizMsgIdr>ADM-2</BizMsgIdr></AppHdr>"
                + "<Document><RltdRef><Ref>UNKNOWN</Ref></RltdRef></Document></Envelope>";

        Admi002MessageDto parsed = Admi002MessageDto.builder()
                .messageType("admi.002.001.01")
                .businessMessageId("ADM-2")
                .originalReference("UNKNOWN")
                .xmlPayload(xml)
                .build();

        when(admi002XmlParserService.parse(xml)).thenReturn(parsed);
        when(admi002Repository.existsByBusinessMessageId("ADM-2")).thenReturn(false);
        when(admi002Repository.existsByOriginalReference("UNKNOWN")).thenReturn(false);
        when(pacs008Repository.findByMessageId("UNKNOWN")).thenReturn(Optional.empty());
        when(pacs008Repository.findTopByTransferIdOrderByCreatedDateDesc("UNKNOWN")).thenReturn(Optional.empty());

        admi002ProcessingService.process(xml);

        ArgumentCaptor<ADMI002> captor = ArgumentCaptor.forClass(ADMI002.class);
        verify(admi002Repository).save(captor.capture());

        ADMI002 saved = captor.getValue();
        assertNull(saved.getTransactionId());
        assertEquals("UNKNOWN", saved.getOriginalReference());
    }
}
