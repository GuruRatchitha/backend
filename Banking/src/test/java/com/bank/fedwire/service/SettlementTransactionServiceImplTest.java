package com.bank.fedwire.service;

import com.bank.fedwire.dto.SettlementAccountResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.PACS002;
import com.bank.fedwire.entity.SettlementTransaction;
import com.bank.fedwire.entity.SettlementTransactionStatus;
import com.bank.fedwire.entity.SettlementTransactionType;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.MessageHeaderRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementTransactionServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private MessageHeaderRepository messageHeaderRepository;

    @Mock
    private SettlementTransactionRepository settlementTransactionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Pacs008XmlGeneratorService pacs008XmlGeneratorService;

    @Mock
    private SnsPublisherService snsPublisherService;

    @InjectMocks
    private SettlementTransactionServiceImpl settlementTransactionService;

    @Test
    void getSettlementAccountMapsExistingSettlementAccount() {
        Account account = Account.builder()
                .accountId(100L)
                .accountNumber("999900001")
                .accountName("ABC Settlement Account")
                .balance(new BigDecimal("2500.00"))
                .accountType("SETTLEMENT")
                .currency("INR")
                .status("ACTIVE")
                .createdDate(LocalDateTime.of(2026, 6, 25, 9, 30))
                .updatedDate(LocalDateTime.of(2026, 6, 26, 10, 15))
                .build();

        when(accountRepository.findByAccountType("SETTLEMENT"))
                .thenReturn(Optional.of(account));

        SettlementAccountResponse response = settlementTransactionService.getSettlementAccount();

        assertEquals(100L, response.accountId());
        assertEquals("999900001", response.accountNumber());
        assertEquals("ABC Settlement Account", response.accountName());
        assertEquals("SETTLEMENT", response.accountType());
        assertEquals("INR", response.currency());
        assertEquals(new BigDecimal("2500.00"), response.balance());
        assertEquals("ACTIVE", response.status());
        assertEquals(LocalDateTime.of(2026, 6, 25, 9, 30), response.createdDate());
        assertEquals(LocalDateTime.of(2026, 6, 26, 10, 15), response.updatedDate());
    }

    @Test
    void processPacs002AcscCompletesTransactionFromTransactionRecord() {
        Transaction transaction = Transaction.builder()
                .transactionId(49L)
                .accountNumber("11111111111")
                .beneficiaryAccountNumber("22222222222")
                .beneficiaryRoutingNumber("031000503")
                .transactionStatus("APPROVED")
                .build();
        PACS002 pacs002 = PACS002.builder()
                .transactionId(49L)
                .transactionStatus("ACSC")
                .messageId("PACS002-1")
                .build();
        SettlementTransaction settlementDebit = SettlementTransaction.builder()
                .paymentId(49L)
                .senderAccount("11111111111")
                .beneficiaryAccount("22222222222")
                .settlementAccount("999900001")
                .amount(new BigDecimal("6.00"))
                .transactionType(SettlementTransactionType.DEBIT_TO_SETTLEMENT)
                .status(SettlementTransactionStatus.SUCCESS)
                .pacs008MessageId("PACS008-1")
                .build();
        Account settlementAccount = Account.builder()
                .accountNumber("999900001")
                .balance(new BigDecimal("6.00"))
                .build();
        Beneficiary beneficiary = Beneficiary.builder()
                .beneficiaryId(7L)
                .accountNumber("22222222222")
                .routingNumber("031000503")
                .status("APPROVED")
                .build();
        MessageHeader messageHeader = MessageHeader.builder()
                .transactionId(49L)
                .messageStatus("APPROVED")
                .build();

        when(transactionRepository.findByTransactionIdForUpdate(49L)).thenReturn(Optional.of(transaction));
        when(settlementTransactionRepository.findTopByPaymentIdAndTransactionTypeOrderByCreatedAtDesc(
                49L, SettlementTransactionType.DEBIT_TO_SETTLEMENT)).thenReturn(Optional.of(settlementDebit));
        when(accountRepository.findByAccountNumberForUpdate("999900001")).thenReturn(Optional.of(settlementAccount));
        when(beneficiaryRepository.findByAccountNumberAndRoutingNumber("22222222222", "031000503"))
                .thenReturn(Optional.of(beneficiary));
        when(settlementTransactionRepository.save(any(SettlementTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(messageHeaderRepository.findTopByTransactionIdOrderByCreatedDateDesc(49L))
                .thenReturn(Optional.of(messageHeader));

        settlementTransactionService.processPacs002(transaction, pacs002);

        assertEquals("COMPLETED", transaction.getTransactionStatus());
        assertEquals("COMPLETED", messageHeader.getMessageStatus());
        assertEquals("ACSC", settlementDebit.getPacs002Status());
        assertEquals(new BigDecimal("0.00"), settlementAccount.getBalance());
    }
}
