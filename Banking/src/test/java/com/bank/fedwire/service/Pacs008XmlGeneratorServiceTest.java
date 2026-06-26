package com.bank.fedwire.service;

import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.PACS008Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Pacs008XmlGeneratorServiceTest {

    @Mock
    private PACS008Repository pacs008Repository;

    @InjectMocks
    private Pacs008XmlGeneratorService pacs008XmlGeneratorService;

    @Test
    void generateXmlRemovesFractionalSecondsAndUsesFixedRoutingAndAccountValues() {
        Transaction transaction = Transaction.builder()
                .transactionId(100L)
                .transactionStatus("APPROVED")
                .beneficiaryRoutingNumber("222222222")
                .build();

        PACS008 pacs008 = PACS008.builder()
                .transactionId(100L)
                .messageId("20260626N1N2G3H4000001")
                .instructionId("INS-20260626-000001")
                .txId("TX-20260626-000001")
                .endToEndId("INV0019253")
                .uetr("123e4567-e89b-12d3-a456-426614174000")
                .amount(new BigDecimal("125.00"))
                .currency("USD")
                .debtorName("Debtor Name")
                .debtorAccount("11111111111")
                .debtorTown("Mumbai")
                .debtorCountry("IN")
                .creditorName("Creditor Name")
                .creditorAccount("99999999999")
                .creditorTown("Chicago")
                .creditorCountry("US")
                .settlementDate(LocalDate.of(2026, 6, 26))
                .acceptanceDatetime(LocalDateTime.of(2026, 6, 26, 5, 6, 53, 306_788_000))
                .chargeBearer("DEBT")
                .localInstrument("CTRC")
                .transaction(transaction)
                .build();

        when(pacs008Repository.findByTransactionId(100L)).thenReturn(Optional.of(pacs008));
        when(pacs008Repository.save(any(PACS008.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String xml = pacs008XmlGeneratorService.generateXml(100L);

        assertTrue(xml.contains("2026-06-26T05:06:53"));
        assertFalse(xml.contains("2026-06-26T05:06:53.306788"));
        assertTrue(xml.contains("<h:BizMsgIdr>20260626N1N2G3H4000001</h:BizMsgIdr>"));
        assertTrue(xml.contains("<p:MsgId>20260626N1N2G3H4000001</p:MsgId>"));
        assertTrue(xml.contains("321171184"));
        assertTrue(xml.contains("091409571"));
        assertTrue(xml.contains("33333333330"));
    }
}
