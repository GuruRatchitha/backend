package com.bank.fedwire.service;

import com.bank.fedwire.dto.SettlementAccountResponse;
import com.bank.fedwire.dto.SettlementTransactionResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.PACS002;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.SettlementTransaction;
import com.bank.fedwire.entity.SettlementTransactionStatus;
import com.bank.fedwire.entity.SettlementTransactionType;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.MessageHeaderRepository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.SettlementTransactionRepository;
import com.bank.fedwire.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private PACS008Repository pacs008Repository;

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
        when(settlementTransactionRepository.sumAmountByTransactionType(SettlementTransactionType.DEBIT_TO_SETTLEMENT))
                .thenReturn(new BigDecimal("1000.00"));
        when(settlementTransactionRepository.sumAmountByTransactionType(SettlementTransactionType.CREDIT_TO_BENEFICIARY))
                .thenReturn(new BigDecimal("400.00"));
        when(settlementTransactionRepository.sumAmountByTransactionType(SettlementTransactionType.RETURN_TO_SENDER))
                .thenReturn(new BigDecimal("100.00"));
        when(settlementTransactionRepository.sumAmountWaitingToRevert())
                .thenReturn(new BigDecimal("125.00"));

        SettlementAccountResponse response = settlementTransactionService.getSettlementAccount();

        assertEquals(100L, response.accountId());
        assertEquals("999900001", response.accountNumber());
        assertEquals("999900001", response.settlementAccountNumber());
        assertEquals("ABC Settlement Account", response.accountName());
        assertEquals("SETTLEMENT", response.accountType());
        assertEquals("INR", response.currency());
        assertEquals(new BigDecimal("2500.00"), response.balance());
        assertEquals(new BigDecimal("375.00"), response.currentBalance());
        assertEquals(new BigDecimal("125.00"), response.revertAmount());
        assertEquals(new BigDecimal("125.00"), response.revertedAmountBalance());
        assertEquals("ACTIVE", response.status());
        assertEquals(LocalDateTime.of(2026, 6, 25, 9, 30), response.createdDate());
        assertEquals(LocalDateTime.of(2026, 6, 26, 10, 15), response.updatedDate());
        assertEquals(List.of(), response.transactionHistory());
        verify(settlementTransactionRepository, never()).findAll(any(Sort.class));
    }

    @Test
    void getSettlementTransactionsMapsDisplayStatusAndHistoryAccountNumber() {
        PageRequest pageable = PageRequest.of(0, 10);
        SettlementTransaction creditedToSettlement = settlementTransaction(
                1L,
                SettlementTransactionType.DEBIT_TO_SETTLEMENT,
                "111111111",
                "222222222");
        SettlementTransaction debitedToBeneficiary = settlementTransaction(
                2L,
                SettlementTransactionType.CREDIT_TO_BENEFICIARY,
                "111111111",
                "222222222");
        SettlementTransaction returnedToSender = settlementTransaction(
                3L,
                SettlementTransactionType.RETURN_TO_SENDER,
                "111111111",
                "222222222");

        when(settlementTransactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(
                        List.of(creditedToSettlement, debitedToBeneficiary, returnedToSender),
                        pageable,
                        3));
        when(pacs008Repository.findByTransactionIdIn(argThat(transactionIds -> transactionIds.contains(7001L))))
                .thenReturn(List.of(PACS008.builder()
                        .transactionId(7001L)
                        .uetr("11111111-2222-3333-4444-555555555555")
                        .debtorName("Sender Name")
                        .creditorName("Receiver Name")
                        .build()));
        when(accountRepository.findByAccountNumberIn(argThat(accountNumbers ->
                accountNumbers.contains("111111111") && accountNumbers.contains("222222222"))))
                .thenReturn(List.of(
                        Account.builder()
                                .accountNumber("111111111")
                                .accountName("Sender Name")
                                .accountType("SAVINGS")
                                .build(),
                        Account.builder()
                                .accountNumber("222222222")
                                .accountName("Receiver Name")
                                .accountType("CURRENT")
                                .build()));

        Page<SettlementTransactionResponse> response = settlementTransactionService.getSettlementTransactions(
                null,
                null,
                null,
                null,
                pageable);

        SettlementTransactionResponse creditedResponse = response.getContent().get(0);
        assertEquals(7001L, creditedResponse.paymentId());
        assertEquals("111111111", creditedResponse.accountNumber());
        assertEquals("111111111", creditedResponse.senderAccountNumber());
        assertEquals("Sender Name", creditedResponse.senderName());
        assertEquals("SAVINGS", creditedResponse.senderAccountType());
        assertEquals("222222222", creditedResponse.beneficiaryAccountNumber());
        assertEquals("222222222", creditedResponse.receiverAccountNumber());
        assertEquals("Receiver Name", creditedResponse.receiverName());
        assertEquals("CURRENT", creditedResponse.receiverAccountType());
        assertEquals("11111111-2222-3333-4444-555555555555", creditedResponse.uetr());
        assertEquals(LocalDateTime.of(2026, 6, 30, 10, 0), creditedResponse.dateTime());
        assertEquals(new BigDecimal("125.00"), creditedResponse.amount());
        assertEquals("Credited", creditedResponse.status());
        assertFalse(creditedResponse.status().contains("Amount"));

        SettlementTransactionResponse beneficiaryResponse = response.getContent().get(1);
        assertEquals(7001L, beneficiaryResponse.paymentId());
        assertEquals("222222222", beneficiaryResponse.accountNumber());
        assertEquals("111111111", beneficiaryResponse.senderAccountNumber());
        assertEquals("222222222", beneficiaryResponse.beneficiaryAccountNumber());
        assertEquals(new BigDecimal("-125.00"), beneficiaryResponse.amount());
        assertEquals("Debited", beneficiaryResponse.status());
        assertFalse(beneficiaryResponse.status().contains("Amount"));

        SettlementTransactionResponse returnedResponse = response.getContent().get(2);
        assertEquals(7001L, returnedResponse.paymentId());
        assertEquals("111111111", returnedResponse.accountNumber());
        assertEquals("111111111", returnedResponse.senderAccountNumber());
        assertEquals("222222222", returnedResponse.beneficiaryAccountNumber());
        assertEquals(new BigDecimal("-125.00"), returnedResponse.amount());
        assertEquals("Returned", returnedResponse.status());
        assertFalse(returnedResponse.status().contains("Amount"));
    }

    @Test
    void processPacs002RejectedMarksTransactionReturnWithoutCreditingSender() {
        Transaction transaction = Transaction.builder()
                .transactionId(7001L)
                .accountNumber("111111111")
                .amount(new BigDecimal("125.00"))
                .transactionStatus("WAITING_FOR_PACS002")
                .beneficiaryAccountNumber("222222222")
                .beneficiaryRoutingNumber("123456789")
                .build();
        PACS002 pacs002 = PACS002.builder()
                .transactionStatus("RJCT")
                .messageId("PACS002-7001")
                .build();
        SettlementTransaction settlementDebit = settlementTransaction(
                1L,
                SettlementTransactionType.DEBIT_TO_SETTLEMENT,
                "111111111",
                "222222222");
        MessageHeader messageHeader = MessageHeader.builder()
                .transactionId(7001L)
                .messageStatus("WAITING_FOR_PACS002")
                .build();

        when(transactionRepository.findByTransactionIdForUpdate(7001L))
                .thenReturn(Optional.of(transaction));
        when(settlementTransactionRepository.findTopByPaymentIdAndTransactionTypeOrderByCreatedAtDesc(
                7001L,
                SettlementTransactionType.DEBIT_TO_SETTLEMENT))
                .thenReturn(Optional.of(settlementDebit));
        when(messageHeaderRepository.findTopByTransactionIdOrderByCreatedDateDesc(7001L))
                .thenReturn(Optional.of(messageHeader));

        settlementTransactionService.processPacs002(transaction, pacs002);

        verify(accountRepository, never()).save(any(Account.class));
        verify(settlementTransactionRepository, never()).save(argThat(saved ->
                saved.getTransactionType() == SettlementTransactionType.RETURN_TO_SENDER));
        assertEquals("RETURN", transaction.getTransactionStatus());
        assertEquals("RETURN", messageHeader.getMessageStatus());
    }

    @Test
    void revertToSenderCreditsSenderDebitsSettlementAndCreatesReturnLedgerRow() {
        Transaction transaction = Transaction.builder()
                .transactionId(7001L)
                .transactionStatus("RETURN")
                .build();
        SettlementTransaction settlementDebit = settlementTransaction(
                1L,
                SettlementTransactionType.DEBIT_TO_SETTLEMENT,
                "111111111",
                "222222222");
        Account settlementAccount = Account.builder()
                .accountNumber("999900001")
                .balance(new BigDecimal("125.00"))
                .build();
        Account senderAccount = Account.builder()
                .accountNumber("111111111")
                .balance(new BigDecimal("25.00"))
                .build();
        MessageHeader messageHeader = MessageHeader.builder()
                .transactionId(7001L)
                .messageStatus("RETURN")
                .build();

        when(transactionRepository.findByTransactionIdForUpdate(7001L))
                .thenReturn(Optional.of(transaction));
        when(settlementTransactionRepository.findTopByPaymentIdAndTransactionTypeOrderByCreatedAtDesc(
                7001L,
                SettlementTransactionType.DEBIT_TO_SETTLEMENT))
                .thenReturn(Optional.of(settlementDebit));
        when(settlementTransactionRepository.existsByPaymentIdAndTransactionTypeAndStatus(
                7001L,
                SettlementTransactionType.RETURN_TO_SENDER,
                SettlementTransactionStatus.SUCCESS))
                .thenReturn(false);
        when(accountRepository.findByAccountNumberForUpdate("999900001"))
                .thenReturn(Optional.of(settlementAccount));
        when(accountRepository.findByAccountNumberForUpdate("111111111"))
                .thenReturn(Optional.of(senderAccount));
        when(messageHeaderRepository.findTopByTransactionIdOrderByCreatedDateDesc(7001L))
                .thenReturn(Optional.of(messageHeader));

        settlementTransactionService.revertToSender(7001L);

        assertEquals(new BigDecimal("0.00"), settlementAccount.getBalance());
        assertEquals(new BigDecimal("150.00"), senderAccount.getBalance());
        verify(settlementTransactionRepository).save(argThat(saved ->
                saved.getTransactionType() == SettlementTransactionType.RETURN_TO_SENDER
                        && saved.getAmount().compareTo(new BigDecimal("125.00")) == 0));
        assertEquals("REVERTED", transaction.getTransactionStatus());
        assertEquals("REVERTED", messageHeader.getMessageStatus());
    }

    private SettlementTransaction settlementTransaction(
            Long settlementTransactionId,
            SettlementTransactionType transactionType,
            String senderAccount,
            String beneficiaryAccount) {
        return SettlementTransaction.builder()
                .settlementTransactionId(settlementTransactionId)
                .paymentId(7001L)
                .senderAccount(senderAccount)
                .beneficiaryAccount(beneficiaryAccount)
                .settlementAccount("999900001")
                .amount(new BigDecimal("125.00"))
                .transactionType(transactionType)
                .status(SettlementTransactionStatus.SUCCESS)
                .pacs008MessageId("PACS008-7001")
                .pacs002Status("ACCP")
                .createdAt(LocalDateTime.of(2026, 6, 30, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 30, 10, 1))
                .build();
    }
}
