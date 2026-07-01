package com.bank.fedwire.service;

import com.bank.fedwire.dto.Admi002MessageDto;
import com.bank.fedwire.entity.ADMI002;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.ADMI002Repository;
import com.bank.fedwire.repository.MessageHeaderRepository;
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
    private MessageHeaderRepository messageHeaderRepository;

    @Mock
    private Admi002ParserService admi002ParserService;

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
                .messageId("ADM-MSG-1")
                .businessMessageId("ADM-1")
                .relatedMessageId("TRF-123")
                .originalReference("TRF-123")
                .errorCode("AC04")
                .errorDescription("Account closed")
                .severity("FATAL")
                .xmlPayload(xml)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(55L)
                .build();

        when(admi002ParserService.parse(xml)).thenReturn(parsed);
        when(admi002Repository.existsByMessageId("ADM-MSG-1")).thenReturn(false);
        when(messageHeaderRepository.findByBusinessMessageId("ADM-1")).thenReturn(Optional.empty());
        when(messageHeaderRepository.findByMessageId("ADM-1")).thenReturn(Optional.empty());
        when(pacs008Repository.findByMessageId("ADM-1")).thenReturn(Optional.empty());
        when(pacs008Repository.findTopByTransferIdOrderByCreatedDateDesc("ADM-1")).thenReturn(Optional.empty());
        when(pacs008Repository.findByTransferId("ADM-1")).thenReturn(Optional.empty());
        when(pacs008Repository.findByTxId("ADM-1")).thenReturn(Optional.empty());
        when(pacs008Repository.findByInstructionId("ADM-1")).thenReturn(Optional.empty());
        when(pacs008Repository.findByEndToEndId("ADM-1")).thenReturn(Optional.empty());
        when(messageHeaderRepository.findByMessageId("TRF-123")).thenReturn(Optional.empty());
        when(messageHeaderRepository.findByBusinessMessageId("TRF-123")).thenReturn(Optional.empty());
        when(pacs008Repository.findByMessageId("TRF-123")).thenReturn(Optional.of(PACS008.builder()
                .transactionId(55L)
                .build()));
        when(transactionRepository.findById(55L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.findByTransactionIdForUpdate(55L)).thenReturn(Optional.of(transaction));
        when(messageHeaderRepository.findTopByTransactionIdOrderByCreatedDateDesc(55L)).thenReturn(Optional.empty());

        admi002ProcessingService.process(xml);

        ArgumentCaptor<ADMI002> captor = ArgumentCaptor.forClass(ADMI002.class);
        verify(admi002Repository).saveAndFlush(captor.capture());

        ADMI002 saved = captor.getValue();
        assertEquals("ADM-MSG-1", saved.getMessageId());
        assertEquals("TRF-123", saved.getOriginalReference());
        assertEquals("ADM-1", saved.getBusinessMessageId());
        assertEquals("TRF-123", saved.getRelatedMessageId());
        assertEquals("AC04", saved.getErrorCode());
        assertEquals("Account closed", saved.getErrorDescription());
        assertEquals("FATAL", saved.getSeverity());
        assertEquals(55L, saved.getTransactionId());
        assertEquals("FAILED", transaction.getTransactionStatus());
        verify(transactionRepository).saveAndFlush(transaction);
    }

    @Test
    void processStoresMessageWhenTransactionCannotBeResolved() {
        String xml = "<Envelope><AppHdr><MsgDefIdr>admi.002.001.01</MsgDefIdr><BizMsgIdr>ADM-2</BizMsgIdr></AppHdr>"
                + "<Document><RltdRef><Ref>UNKNOWN</Ref></RltdRef></Document></Envelope>";

        Admi002MessageDto parsed = Admi002MessageDto.builder()
                .messageType("admi.002.001.01")
                .messageId("ADM-MSG-2")
                .businessMessageId("ADM-2")
                .relatedMessageId("UNKNOWN")
                .originalReference("UNKNOWN")
                .xmlPayload(xml)
                .build();

        when(admi002ParserService.parse(xml)).thenReturn(parsed);
        when(admi002Repository.existsByMessageId("ADM-MSG-2")).thenReturn(false);
        when(messageHeaderRepository.findByMessageId("ADM-2")).thenReturn(Optional.empty());
        when(messageHeaderRepository.findByBusinessMessageId("ADM-2")).thenReturn(Optional.empty());
        when(pacs008Repository.findByMessageId("ADM-2")).thenReturn(Optional.empty());
        when(pacs008Repository.findTopByTransferIdOrderByCreatedDateDesc("ADM-2")).thenReturn(Optional.empty());
        when(pacs008Repository.findByTransferId("ADM-2")).thenReturn(Optional.empty());
        when(pacs008Repository.findByTxId("ADM-2")).thenReturn(Optional.empty());
        when(pacs008Repository.findByInstructionId("ADM-2")).thenReturn(Optional.empty());
        when(pacs008Repository.findByEndToEndId("ADM-2")).thenReturn(Optional.empty());
        when(messageHeaderRepository.findByMessageId("UNKNOWN")).thenReturn(Optional.empty());
        when(messageHeaderRepository.findByBusinessMessageId("UNKNOWN")).thenReturn(Optional.empty());
        when(pacs008Repository.findByMessageId("UNKNOWN")).thenReturn(Optional.empty());
        when(pacs008Repository.findTopByTransferIdOrderByCreatedDateDesc("UNKNOWN")).thenReturn(Optional.empty());
        when(pacs008Repository.findByTransferId("UNKNOWN")).thenReturn(Optional.empty());
        when(pacs008Repository.findByTxId("UNKNOWN")).thenReturn(Optional.empty());
        when(pacs008Repository.findByInstructionId("UNKNOWN")).thenReturn(Optional.empty());
        when(pacs008Repository.findByEndToEndId("UNKNOWN")).thenReturn(Optional.empty());

        admi002ProcessingService.process(xml);

        ArgumentCaptor<ADMI002> captor = ArgumentCaptor.forClass(ADMI002.class);
        verify(admi002Repository).saveAndFlush(captor.capture());

        ADMI002 saved = captor.getValue();
        assertNull(saved.getTransactionId());
        assertEquals("UNKNOWN", saved.getOriginalReference());
    }
}
