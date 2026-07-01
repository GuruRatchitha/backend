package com.bank.fedwire.service;

import com.bank.fedwire.dto.TransactionDetailResponse;
import com.bank.fedwire.dto.EmployeeTransactionQueueResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.ADMI002;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.PACS002;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.ADMI002Repository;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.DashboardActivityRepository;
import com.bank.fedwire.repository.EmployeeTransactionQueueProjection;
import com.bank.fedwire.repository.MessageHeaderRepository;
import com.bank.fedwire.repository.PACS002Repository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.SettlementTransactionRepository;
import com.bank.fedwire.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private MessageHeaderRepository messageHeaderRepository;

    @Mock
    private PACS002Repository pacs002Repository;

    @Mock
    private ADMI002Repository admi002Repository;

    @Mock
    private PACS008Repository pacs008Repository;

    @Mock
    private SettlementTransactionService settlementTransactionService;

    @Mock
    private SettlementTransactionRepository settlementTransactionRepository;

    @Mock
    private DashboardActivityRepository dashboardActivityRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    void detailsIncludesPacs008AndPacs002ForRejectedPayment() {
        Transaction transaction = baseTransaction("REJECTED");
        mockDetailDependencies(transaction);
        when(pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(49L))
                .thenReturn(Optional.of(PACS008.builder().xmlPayload("<PACS008/>").build()));
        when(pacs002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L))
                .thenReturn(Optional.of(PACS002.builder().xmlPayload("<PACS002><TxSts>RJCT</TxSts></PACS002>").build()));
        when(admi002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());

        TransactionDetailResponse response = transactionService.getEmployeeTransactionDetails(49L);

        assertEquals("REJECTED", response.getStatus());
        assertEquals("REJECTED", response.getPaymentDetails().getStatus());
        assertEquals("<PACS008/>", response.getXmlMessages().getPacs008());
        assertEquals("<PACS002><TxSts>RJCT</TxSts></PACS002>", response.getXmlMessages().getPacs002());
        assertNull(response.getXmlMessages().getAdmi002());
        assertEquals("<PACS008/>", response.getPacs008Xml());
        assertEquals("<PACS002><TxSts>RJCT</TxSts></PACS002>", response.getPacs002Xml());
        assertNull(response.getAdmi002Xml());
        assertEquals(true, response.getHasPacs008());
        assertEquals(true, response.getHasPacs002());
        assertEquals(false, response.getHasAdmi002());
    }

    @Test
    void detailsIncludesPacs008AndAdmi002ForSyntaxFailure() {
        Transaction transaction = baseTransaction("REJECTED");
        mockDetailDependencies(transaction);
        when(pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(49L))
                .thenReturn(Optional.of(PACS008.builder().xmlPayload("<PACS008/>").build()));
        when(pacs002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());
        when(admi002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L))
                .thenReturn(Optional.of(ADMI002.builder().xmlPayload("<ADMI002/>").build()));

        TransactionDetailResponse response = transactionService.getEmployeeTransactionDetails(49L);

        assertEquals("REJECTED", response.getStatus());
        assertEquals("<PACS008/>", response.getXmlMessages().getPacs008());
        assertNull(response.getXmlMessages().getPacs002());
        assertEquals("<ADMI002/>", response.getXmlMessages().getAdmi002());
        assertEquals(true, response.getHasPacs008());
        assertEquals(false, response.getHasPacs002());
        assertEquals(true, response.getHasAdmi002());
    }

    @Test
    void detailsReturnsCurrentTransactionStatusFromRecord() {
        Transaction transaction = baseTransaction("COMPLETED");
        mockDetailDependencies(transaction);
        when(pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(49L)).thenReturn(Optional.empty());
        when(pacs002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());
        when(admi002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());

        TransactionDetailResponse response = transactionService.getEmployeeTransactionDetails(49L);

        assertEquals("COMPLETED", response.getStatus());
        assertEquals("COMPLETED", response.getPaymentDetails().getStatus());
        assertEquals(false, response.getHasPacs008());
        assertEquals(false, response.getHasPacs002());
        assertEquals(false, response.getHasAdmi002());
        assertNull(response.getPacs008Xml());
        assertNull(response.getPacs002Xml());
        assertNull(response.getAdmi002Xml());
    }

    @Test
    void detailsEnrichmentHandlesOnlyPacs008Generated() {
        Transaction transaction = baseTransaction("WAITING_FOR_PACS002");
        mockDetailDependencies(transaction);
        String pacs008Xml = """
                <Document xmlns:p="urn:fedwire">
                    <p:UETR>123e4567-e89b-12d3-a456-426614174000</p:UETR>
                </Document>
                """;
        when(pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(49L))
                .thenReturn(Optional.of(PACS008.builder()
                        .uetr("db-uetr")
                        .xmlPayload(pacs008Xml)
                        .build()));
        when(pacs002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());
        when(admi002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());

        TransactionDetailResponse response = transactionService.getEmployeeTransactionDetails(49L);

        assertEquals(true, response.getHasPacs008());
        assertEquals(false, response.getHasPacs002());
        assertEquals(false, response.getHasAdmi002());
        assertEquals(pacs008Xml, response.getPacs008Xml());
        assertNull(response.getPacs002Xml());
        assertNull(response.getAdmi002Xml());
        assertNull(response.getPacs002Reason());
        assertNull(response.getAdmi002Reason());
        assertEquals("db-uetr", response.getUetr());
        assertEquals("db-uetr", response.getPaymentDetails().getUetr());
        assertEquals(LocalDateTime.of(2026, 6, 30, 10, 0), response.getPaymentTimestamp());
    }

    @Test
    void detailsEnrichmentHandlesPacs002Accepted() {
        Transaction transaction = baseTransaction("PROCESSING");
        mockDetailDependencies(transaction);
        String pacs002Xml = "<Document xmlns:p=\"urn:fedwire\"><p:TxSts>ACSC</p:TxSts></Document>";
        when(pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(49L))
                .thenReturn(Optional.of(PACS008.builder().xmlPayload("<PACS008/>").build()));
        when(pacs002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L))
                .thenReturn(Optional.of(PACS002.builder()
                        .transactionStatus("ACSC")
                        .xmlPayload(pacs002Xml)
                        .build()));
        when(admi002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());

        TransactionDetailResponse response = transactionService.getEmployeeTransactionDetails(49L);

        assertEquals(true, response.getHasPacs008());
        assertEquals(true, response.getHasPacs002());
        assertEquals(false, response.getHasAdmi002());
        assertEquals(pacs002Xml, response.getPacs002Xml());
        assertNull(response.getPacs002Reason());
        assertNull(response.getAdmi002Reason());
    }

    @Test
    void detailsEnrichmentParsesPacs002RejectedReason() {
        Transaction transaction = baseTransaction("REJECTED");
        mockDetailDependencies(transaction);
        String pacs002Xml = """
                <Document xmlns:p="urn:fedwire">
                    <p:TxSts>RJCT</p:TxSts>
                    <p:AddtlInf>
                        Creditor or Instructed or Receiving routing number is not found.
                    </p:AddtlInf>
                </Document>
                """;
        when(pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(49L))
                .thenReturn(Optional.of(PACS008.builder().xmlPayload("<PACS008/>").build()));
        when(pacs002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L))
                .thenReturn(Optional.of(PACS002.builder()
                        .transactionStatus("RJCT")
                        .xmlPayload(pacs002Xml)
                        .build()));
        when(admi002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());

        TransactionDetailResponse response = transactionService.getEmployeeTransactionDetails(49L);

        assertEquals("Creditor or Instructed or Receiving routing number is not found.",
                response.getPacs002Reason());
        assertEquals("Creditor or Instructed or Receiving routing number is not found.",
                response.getRejectionReason());
        assertNull(response.getAdmi002Reason());
    }

    @Test
    void detailsEnrichmentParsesAdmi002Reason() {
        Transaction transaction = baseTransaction("FAILED");
        mockDetailDependencies(transaction);
        String admi002Xml = """
                <Document xmlns:p="urn:fedwire">
                    <p:RsnDesc>Amt field validation failed</p:RsnDesc>
                </Document>
                """;
        when(pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(49L))
                .thenReturn(Optional.of(PACS008.builder().xmlPayload("<PACS008/>").build()));
        when(pacs002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());
        when(admi002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L))
                .thenReturn(Optional.of(ADMI002.builder()
                        .xmlPayload(admi002Xml)
                        .build()));

        TransactionDetailResponse response = transactionService.getEmployeeTransactionDetails(49L);

        assertEquals(true, response.getHasAdmi002());
        assertEquals(admi002Xml, response.getAdmi002Xml());
        assertEquals("Amt field validation failed", response.getAdmi002Reason());
        assertNull(response.getPacs002Reason());
    }

    @Test
    void detailsEnrichmentParsesUetrFromPacs008XmlWhenDatabaseValueMissing() {
        Transaction transaction = baseTransaction("WAITING_FOR_PACS002");
        mockDetailDependencies(transaction);
        when(pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(49L))
                .thenReturn(Optional.of(PACS008.builder()
                        .xmlPayload("<Document xmlns:p=\"urn:fedwire\"><p:UETR>xml-uetr</p:UETR></Document>")
                        .build()));
        when(pacs002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());
        when(admi002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(49L)).thenReturn(Optional.empty());

        TransactionDetailResponse response = transactionService.getEmployeeTransactionDetails(49L);

        assertEquals("xml-uetr", response.getUetr());
    }

    @Test
    void queueReturnsCurrentTransactionStatusesFromRepository() {
        List<EmployeeTransactionQueueProjection> queue = List.of(
                queueProjection(1L, "PENDING"),
                queueProjection(2L, "APPROVED"),
                queueProjection(3L, "REJECTED"),
                queueProjection(4L, "COMPLETED"));
        when(transactionRepository.findEmployeeTransactionQueue(anyInt())).thenReturn(queue);

        List<EmployeeTransactionQueueResponse> response = transactionService.getEmployeeTransactionQueue();

        assertEquals("PENDING", response.get(0).getStatus());
        assertEquals("APPROVED", response.get(1).getStatus());
        assertEquals("REJECTED", response.get(2).getStatus());
        assertEquals("COMPLETED", response.get(3).getStatus());
        assertEquals(LocalDateTime.of(2026, 6, 30, 10, 0), response.get(0).getPaymentTimestamp());
    }

    private void mockDetailDependencies(Transaction transaction) {
        when(transactionRepository.findById(49L)).thenReturn(Optional.of(transaction));
        when(accountRepository.findByAccountNumber("11111111111")).thenReturn(Optional.of(Account.builder()
                .accountNumber("11111111111")
                .routingNumber("021000021")
                .currency("USD")
                .user(User.builder()
                        .userName("Sender")
                        .countryCode("US")
                        .build())
                .build()));
        when(beneficiaryRepository.findByAccountNumberAndRoutingNumber("22222222222", "031000503"))
                .thenReturn(Optional.of(Beneficiary.builder()
                        .beneficiaryName("Receiver")
                        .accountNumber("22222222222")
                        .routingNumber("031000503")
                        .countryCode("US")
                        .build()));
    }

    private EmployeeTransactionQueueProjection queueProjection(Long transactionId, String status) {
        return new EmployeeTransactionQueueProjection() {
            @Override
            public Long getTransactionId() {
                return transactionId;
            }

            @Override
            public String getTransactionReference() {
                return String.valueOf(transactionId);
            }

            @Override
            public String getSenderName() {
                return "Sender";
            }

            @Override
            public String getBeneficiaryName() {
                return "Receiver";
            }

            @Override
            public BigDecimal getAmount() {
                return new BigDecimal("125.00");
            }

            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public LocalDateTime getPaymentDate() {
                return LocalDateTime.of(2026, 6, 30, 10, 0);
            }
        };
    }

    private Transaction baseTransaction(String status) {
        return Transaction.builder()
                .transactionId(49L)
                .accountNumber("11111111111")
                .beneficiaryName("Receiver")
                .beneficiaryAccountNumber("22222222222")
                .beneficiaryRoutingNumber("031000503")
                .amount(new BigDecimal("125.00"))
                .transactionDateTime(LocalDateTime.of(2026, 6, 30, 10, 0))
                .transactionStatus(status)
                .build();
    }
}
