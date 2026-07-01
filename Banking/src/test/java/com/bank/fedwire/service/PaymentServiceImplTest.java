package com.bank.fedwire.service;

import com.bank.fedwire.dto.PaymentRequest;
import com.bank.fedwire.dto.PaymentResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.MessageHeaderRepository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.TransactionRepository;
import com.bank.fedwire.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MessageHeaderRepository messageHeaderRepository;

    @Mock
    private PACS008Repository pacs008Repository;

    @Mock
    private IdGenerationService idGenerationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void initiatePaymentNormalizesExistingCustomerSnapshotBeforeSavingPacs008() {
        User sender = User.builder()
                .userId(7L)
                .userName("Dravid545")
                .townName("A very long customer town name beyond limit")
                .countryCode("India")
                .build();
        Account senderAccount = Account.builder()
                .accountNumber("21234567890")
                .accountType("SAVINGS")
                .currency("usd")
                .status("ACTIVE")
                .user(sender)
                .build();
        Beneficiary beneficiary = Beneficiary.builder()
                .beneficiaryId(10L)
                .beneficiaryName("Receiver")
                .townName("Phoenix")
                .countryCode("US")
                .accountNumber("99887766554")
                .routingNumber("021000021")
                .status("APPROVED")
                .build();

        when(beneficiaryRepository.findByIdForPaymentUpdate(10L)).thenReturn(Optional.of(beneficiary));
        when(userRepository.findById(7L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByUserUserId(7L)).thenReturn(List.of(senderAccount));
        when(accountRepository.findByAccountNumber("21234567890")).thenReturn(Optional.of(senderAccount));
        when(idGenerationService.generateBusinessMessageId()).thenReturn("20260701ABCDEF000001");
        when(idGenerationService.generateTransferId()).thenReturn("TRF-20260701ABCDEF000001");
        when(idGenerationService.generatePaymentTransactionId()).thenReturn("PMT-20260701ABCDEF000001");
        when(idGenerationService.generateBankTransactionId()).thenReturn("20260701ABCDEF000002");
        when(idGenerationService.generateInstructionId()).thenReturn("INS-20260701ABCDEF000001");
        when(idGenerationService.generateTxId()).thenReturn("TX-20260701ABCDEF000001");
        when(idGenerationService.generateEndToEndId()).thenReturn("INV0019253");
        when(idGenerationService.generateUetr()).thenReturn("123e4567-e89b-12d3-a456-426614174000");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setTransactionId(99L);
            return transaction;
        });
        when(messageHeaderRepository.save(any(MessageHeader.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pacs008Repository.save(any(PACS008.class))).thenAnswer(invocation -> {
            PACS008 pacs008 = invocation.getArgument(0);
            pacs008.setPacs008Id(123L);
            return pacs008;
        });

        PaymentResponse response = paymentService.initiatePayment(7L,
                new PaymentRequest(new BigDecimal("125.00"), 10L));

        ArgumentCaptor<PACS008> pacs008Captor = ArgumentCaptor.forClass(PACS008.class);
        verify(pacs008Repository).save(pacs008Captor.capture());
        PACS008 savedPacs008 = pacs008Captor.getValue();
        assertThat(response.getTransactionId()).isEqualTo(99L);
        assertThat(savedPacs008.getTransactionId()).isEqualTo(99L);
        assertThat(savedPacs008.getDebtorTown()).isEqualTo("A very long customer town name beyo");
        assertThat(savedPacs008.getDebtorTown()).hasSize(35);
        assertThat(savedPacs008.getDebtorCountry()).isEqualTo("IN");
    }
}
