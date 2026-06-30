package com.bank.fedwire.service;

import com.bank.fedwire.dto.SettlementAccountResponse;
import com.bank.fedwire.dto.SettlementTransactionResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.SettlementTransaction;
import com.bank.fedwire.entity.SettlementTransactionStatus;
import com.bank.fedwire.entity.SettlementTransactionType;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.MessageHeaderRepository;
import com.bank.fedwire.repository.SettlementTransactionRepository;
import com.bank.fedwire.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.ArgumentMatchers.eq;
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
        assertEquals("222222222", creditedResponse.beneficiaryAccountNumber());
        assertEquals("Credited To Settlement", creditedResponse.status());
        assertFalse(creditedResponse.status().contains("Amount"));

        SettlementTransactionResponse beneficiaryResponse = response.getContent().get(1);
        assertEquals(7001L, beneficiaryResponse.paymentId());
        assertEquals("222222222", beneficiaryResponse.accountNumber());
        assertEquals("111111111", beneficiaryResponse.senderAccountNumber());
        assertEquals("222222222", beneficiaryResponse.beneficiaryAccountNumber());
        assertEquals("Debited From Settlement", beneficiaryResponse.status());
        assertFalse(beneficiaryResponse.status().contains("Amount"));

        SettlementTransactionResponse returnedResponse = response.getContent().get(2);
        assertEquals(7001L, returnedResponse.paymentId());
        assertEquals("111111111", returnedResponse.accountNumber());
        assertEquals("111111111", returnedResponse.senderAccountNumber());
        assertEquals("222222222", returnedResponse.beneficiaryAccountNumber());
        assertEquals("Debited From Settlement", returnedResponse.status());
        assertFalse(returnedResponse.status().contains("Amount"));
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
