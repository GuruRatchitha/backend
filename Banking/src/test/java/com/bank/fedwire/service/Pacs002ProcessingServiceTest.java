package com.bank.fedwire.service;

import com.bank.fedwire.dto.Pacs002MessageDto;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.PACS002;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.MessageHeaderRepository;
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
    private MessageHeaderRepository messageHeaderRepository;

    @Mock
    private Pacs002XmlParserService pacs002XmlParserService;

    @InjectMocks
    private Pacs002ProcessingService pacs002ProcessingService;

    @Test
    void processMapsPendingToProcessingAndUpdatesTransactionAndHeader() {
        String xml = "<Document><original_message_id>MSG-1</original_message_id>"
                + "<message_id>MSG-2</message_id>"
                + "<transfer_id>TRF-123</transfer_id>"
                + "<transaction_status>PDNG</transaction_status>"
                + "<reason_code>WAIT</reason_code></Document>";

        Pacs002MessageDto parsed = Pacs002MessageDto.builder()
                .originalMessageId("MSG-1")
                .messageId("MSG-2")
                .transferId("TRF-123")
                .transactionStatus("PDNG")
                .reasonCode("WAIT")
                .xmlPayload(xml)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionId(88L)
                .transferId("TRF-123")
                .transactionStatus("APPROVED")
                .build();

        MessageHeader messageHeader = MessageHeader.builder()
                .messageId("MSG-2")
                .transactionId(88L)
                .messageStatus("APPROVED")
                .build();

        when(pacs002XmlParserService.parse(xml)).thenReturn(parsed);
        when(pacs002Repository.existsByMessageId("MSG-2")).thenReturn(false);
        when(pacs002Repository.existsByOriginalMessageId("MSG-1")).thenReturn(false);
        when(pacs002Repository.existsByTransferId("TRF-123")).thenReturn(false);
        when(transactionRepository.findByTransferId("TRF-123")).thenReturn(Optional.of(transaction));
        when(messageHeaderRepository.findByTransactionId(88L)).thenReturn(Optional.of(messageHeader));

        pacs002ProcessingService.process(xml);

        assertEquals("PROCESSING", transaction.getTransactionStatus());
        assertEquals("PROCESSING", messageHeader.getMessageStatus());

        ArgumentCaptor<PACS002> captor = ArgumentCaptor.forClass(PACS002.class);
        verify(pacs002Repository).save(captor.capture());

        PACS002 saved = captor.getValue();
        assertEquals(88L, saved.getTransactionId());
        assertEquals("PROCESSING", saved.getTransactionStatus());
        assertEquals("WAIT", saved.getReasonCode());
        assertEquals(xml, saved.getXmlPayload());

        verify(transactionRepository).save(transaction);
        verify(messageHeaderRepository).save(messageHeader);
        assertFalse(saved.getXmlPayload().isBlank());
    }
}
