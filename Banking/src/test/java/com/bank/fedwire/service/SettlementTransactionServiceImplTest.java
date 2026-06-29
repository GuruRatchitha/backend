package com.bank.fedwire.service;

import com.bank.fedwire.dto.SettlementAccountResponse;
import com.bank.fedwire.entity.Account;
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
}
