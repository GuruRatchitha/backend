package com.bank.fedwire.service;

import com.bank.fedwire.dto.Pacs002MessageDto;
import com.bank.fedwire.entity.PACS002;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.PACS002Repository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Pacs002ProcessingServiceTest {

    @Mock
    private PACS002Repository pacs002Repository;

    @Mock
    private PACS008Repository pacs008Repository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Pacs002XmlParserService pacs002XmlParserService;

    @Mock
    private SettlementTransactionService settlementTransactionService;

    @InjectMocks
    private Pacs002ProcessingService pacs002ProcessingService;

    @Test
    void processAccpDelegatesToSettlementServiceAndPersistsPacs002() {
        String xml = "<Document><original_message_id>MSG-1</original_message_id>"
                + "<message_id>MSG-2</message_id>"
                + "<transfer_id>TRF-123</transfer_id>"
                + "<transaction_status>ACCP</transaction_status>"
                + "<reason_code>WAIT</reason_code></Document>";

        Pacs002MessageDto parsed = Pacs002MessageDto.builder()
                .originalMessageId("MSG-1")
                .messageId("MSG-2")
                .transferId("TRF-123")
                .transactionStatus("ACCP")
                .reasonCode("WAIT")
                .xmlPayload(xml)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(88L)
                .transferId("TRF-123")
                .transactionStatus("WAITING_FOR_PACS002")
                .build();

        when(pacs002XmlParserService.parse(xml)).thenReturn(parsed);
        when(pacs002Repository.existsByMessageId("MSG-2")).thenReturn(false);
        when(transactionRepository.findByTransferId("TRF-123")).thenReturn(Optional.of(transaction));
        doNothing().when(settlementTransactionService).processPacs002(any(Transaction.class), any(PACS002.class));

        pacs002ProcessingService.process(xml);

        assertEquals("WAITING_FOR_PACS002", transaction.getTransactionStatus());

        ArgumentCaptor<PACS002> captor = ArgumentCaptor.forClass(PACS002.class);
        verify(pacs002Repository).saveAndFlush(captor.capture());

        PACS002 saved = captor.getValue();
        assertEquals(88L, saved.getTransactionId());
        assertEquals("ACCP", saved.getTransactionStatus());
        assertEquals("WAIT", saved.getReasonCode());
        assertEquals(xml, saved.getXmlPayload());

        verify(settlementTransactionService).processPacs002(transaction, saved);
        assertFalse(saved.getXmlPayload().isBlank());
    }

    @Test
    void processResolvesTransactionFromMessageIdWhenTransferIdIsMissing() {
        String xml = "<Document><message_id>MSG-2</message_id>"
                + "<transaction_status>RJCT</transaction_status>"
                + "<reason_code>AC04</reason_code></Document>";

        Pacs002MessageDto parsed = Pacs002MessageDto.builder()
                .messageId("MSG-2")
                .transactionStatus("RJCT")
                .reasonCode("AC04")
                .xmlPayload(xml)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(99L)
                .transactionStatus("WAITING_FOR_PACS002")
                .build();

        when(pacs002XmlParserService.parse(xml)).thenReturn(parsed);
        when(pacs002Repository.existsByMessageId("MSG-2")).thenReturn(false);
        when(pacs008Repository.findByMessageId("MSG-2")).thenReturn(Optional.of(
                PACS008.builder().transactionId(99L).build()));
        when(transactionRepository.findById(99L)).thenReturn(Optional.of(transaction));
        doNothing().when(settlementTransactionService).processPacs002(any(Transaction.class), any(PACS002.class));

        pacs002ProcessingService.process(xml);

        ArgumentCaptor<PACS002> captor = ArgumentCaptor.forClass(PACS002.class);
        verify(pacs002Repository).saveAndFlush(captor.capture());

        PACS002 saved = captor.getValue();
        assertEquals(99L, saved.getTransactionId());
        assertEquals("RJCT", saved.getTransactionStatus());
        assertEquals("AC04", saved.getReasonCode());
        verify(settlementTransactionService).processPacs002(transaction, saved);
    }

    @Test
    void processTreatsAcsCAsAcceptedSettlementCompleted() {
        String xml = "<Document><transfer_id>TRF-123</transfer_id>"
                + "<transaction_status>ACSC</transaction_status>"
                + "<reason_code>ACSP</reason_code></Document>";

        Pacs002MessageDto parsed = Pacs002MessageDto.builder()
                .transferId("TRF-123")
                .transactionStatus("ACSC")
                .reasonCode("ACSP")
                .xmlPayload(xml)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(100L)
                .transferId("TRF-123")
                .transactionStatus("WAITING_FOR_PACS002")
                .build();

        when(pacs002XmlParserService.parse(xml)).thenReturn(parsed);
        when(transactionRepository.findByTransferId("TRF-123")).thenReturn(Optional.of(transaction));
        doNothing().when(settlementTransactionService).processPacs002(any(Transaction.class), any(PACS002.class));

        pacs002ProcessingService.process(xml);

        ArgumentCaptor<PACS002> captor = ArgumentCaptor.forClass(PACS002.class);
        verify(pacs002Repository).saveAndFlush(captor.capture());

        PACS002 saved = captor.getValue();
        assertEquals(100L, saved.getTransactionId());
        assertEquals("ACCP", saved.getTransactionStatus());
        assertEquals("ACSP", saved.getReasonCode());
        verify(settlementTransactionService).processPacs002(transaction, saved);
    }

    @Test
    void processStoresPacs002XmlEvenWhenTransactionCannotBeResolved() {
        String xml = "<Document><message_id>FEDWIRE-STATUS-1</message_id>"
                + "<original_message_id>UNKNOWN-PACS008</original_message_id>"
                + "<transfer_id>UNKNOWN-TX</transfer_id>"
                + "<transaction_status>ACSC</transaction_status></Document>";

        Pacs002MessageDto parsed = Pacs002MessageDto.builder()
                .messageId("FEDWIRE-STATUS-1")
                .originalMessageId("UNKNOWN-PACS008")
                .transferId("UNKNOWN-TX")
                .transactionStatus("ACSC")
                .xmlPayload(xml)
                .build();

        when(pacs002XmlParserService.parse(xml)).thenReturn(parsed);
        when(pacs002Repository.existsByMessageId("FEDWIRE-STATUS-1")).thenReturn(false);
        when(transactionRepository.findByTransferId("UNKNOWN-TX")).thenReturn(Optional.empty());
        when(pacs008Repository.findByTransferId("UNKNOWN-TX")).thenReturn(Optional.empty());
        when(pacs008Repository.findByTxId("UNKNOWN-TX")).thenReturn(Optional.empty());
        when(pacs008Repository.findByInstructionId("UNKNOWN-TX")).thenReturn(Optional.empty());
        when(pacs008Repository.findByEndToEndId("UNKNOWN-TX")).thenReturn(Optional.empty());
        when(pacs008Repository.findByMessageId("UNKNOWN-PACS008")).thenReturn(Optional.empty());

        pacs002ProcessingService.process(xml);

        ArgumentCaptor<PACS002> captor = ArgumentCaptor.forClass(PACS002.class);
        verify(pacs002Repository).saveAndFlush(captor.capture());

        PACS002 saved = captor.getValue();
        assertEquals("FEDWIRE-STATUS-1", saved.getMessageId());
        assertEquals("UNKNOWN-PACS008", saved.getOriginalMessageId());
        assertEquals("UNKNOWN-TX", saved.getTransferId());
        assertEquals("ACCP", saved.getTransactionStatus());
        assertEquals(xml, saved.getXmlPayload());
        assertEquals(null, saved.getTransactionId());
        verify(settlementTransactionService, never()).processPacs002(any(), any());
    }
}
