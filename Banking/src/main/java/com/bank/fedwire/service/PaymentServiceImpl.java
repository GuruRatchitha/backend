package com.bank.fedwire.service;

import com.bank.fedwire.dto.PaymentRequest;
import com.bank.fedwire.dto.PaymentResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.entity.TransactionStatus;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.MessageHeaderRepository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String MESSAGE_TYPE = "pacs.008.001.08";
    private static final String DIRECTION = "OUTGOING";
    private static final String TRANSACTION_TYPE = "DEBIT";
    private static final String CHARGE_BEARER = "DEBT";
    private static final String LOCAL_INSTRUMENT = "CTRC";

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final MessageHeaderRepository messageHeaderRepository;
    private final PACS008Repository pacs008Repository;
    private final IdGenerationService idGenerationService;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        validateRequest(request);

        Beneficiary beneficiary = beneficiaryRepository.findByIdForPaymentUpdate(request.getBeneficiaryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
        validateBeneficiaryStatus(beneficiary);

        User sender = beneficiary.getUser();
        if (sender == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Beneficiary is not linked to a user");
        }

        Account senderAccount = selectSenderAccount(sender.getUserId());
        String pendingPaymentKey = createPendingPaymentKey(sender.getUserId(), beneficiary.getBeneficiaryId());
        PaymentResponse existingPayment = transactionRepository.findByPendingPaymentKey(pendingPaymentKey)
                .map(this::toPaymentResponse)
                .orElse(null);
        if (existingPayment != null) {
            return existingPayment;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        String messageId = idGenerationService.generateBusinessMessageId();
        String transferId = idGenerationService.generateTransferId();
        String paymentTransactionId = idGenerationService.generatePaymentTransactionId();
        String bankTransactionId = idGenerationService.generateBankTransactionId();

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .transferId(transferId)
                .paymentTransactionId(paymentTransactionId)
                .bankTransactionId(bankTransactionId)
                .accountNumber(senderAccount.getAccountNumber())
                .amount(request.getAmount())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .beneficiaryAccountNumber(beneficiary.getAccountNumber())
                .beneficiaryRoutingNumber(beneficiary.getRoutingNumber())
                .pendingPaymentKey(pendingPaymentKey)
                .remarks(MESSAGE_TYPE)
                .transactionDateTime(now)
                .transactionStatus(TransactionStatus.PENDING.name())
                .transactionType(TRANSACTION_TYPE)
                .build());

        MessageHeader messageHeader = messageHeaderRepository.save(MessageHeader.builder()
                .messageId(messageId)
                .businessMessageId(messageId)
                .messageType(MESSAGE_TYPE)
                .direction(DIRECTION)
                .messageStatus(TransactionStatus.PENDING.name())
                .createdDate(now)
                .transactionId(transaction.getTransactionId())
                .build());

        PACS008 pacs008 = pacs008Repository.save(PACS008.builder()
                .transactionId(transaction.getTransactionId())
                .messageId(messageHeader.getMessageId())
                .transferId(transferId)
                .instructionId(idGenerationService.generateInstructionId())
                .txId(idGenerationService.generateTxId())
                .endToEndId(idGenerationService.generateEndToEndId())
                .uetr(idGenerationService.generateUetr())
                .paymentTransactionId(paymentTransactionId)
                .bankTransactionId(bankTransactionId)
                .amount(request.getAmount())
                .currency(resolveCurrency(senderAccount))
                .debtorName(requireSnapshotValue(sender.getUserName(), "Sender name is required"))
                .debtorAccount(requireSnapshotValue(senderAccount.getAccountNumber(), "Sender account number is required"))
                .debtorTown(requireSnapshotValue(sender.getTownName(), "Sender town name is required"))
                .debtorCountry(normalizeCountry(sender.getCountryCode()))
                .creditorName(requireSnapshotValue(beneficiary.getBeneficiaryName(), "Beneficiary name is required"))
                .creditorAccount(requireSnapshotValue(beneficiary.getAccountNumber(), "Beneficiary account number is required"))
                .creditorTown(requireSnapshotValue(beneficiary.getTownName(), "Beneficiary town name is required"))
                .creditorCountry(normalizeCountry(beneficiary.getCountryCode()))
                .settlementDate(now.toLocalDate())
                .acceptanceDatetime(now)
                .chargeBearer(CHARGE_BEARER)
                .localInstrument(LOCAL_INSTRUMENT)
                .xmlPayload(null)
                .createdDate(now)
                .build());

        try {
            return toPaymentResponse(transaction, messageHeader, pacs008);
        } catch (DataIntegrityViolationException ex) {
            return transactionRepository.findByPendingPaymentKey(pendingPaymentKey)
                    .map(this::toPaymentResponse)
                    .orElseThrow(() -> ex);
        }
    }

    private PaymentResponse toPaymentResponse(Transaction transaction) {
        MessageHeader messageHeader = messageHeaderRepository.findByTransactionId(transaction.getTransactionId())
                .orElse(null);
        PACS008 pacs008 = pacs008Repository.findByTransactionId(transaction.getTransactionId())
                .orElse(null);

        return toPaymentResponse(transaction, messageHeader, pacs008);
    }

    private PaymentResponse toPaymentResponse(Transaction transaction, MessageHeader messageHeader, PACS008 pacs008) {
        return PaymentResponse.builder()
                .transactionId(transaction.getTransactionId())
                .transferId(transaction.getTransferId())
                .paymentTransactionId(transaction.getPaymentTransactionId())
                .bankTransactionId(transaction.getBankTransactionId())
                .messageId(messageHeader != null ? messageHeader.getMessageId() : null)
                .businessMessageId(messageHeader != null ? messageHeader.getBusinessMessageId() : null)
                .pacs008Id(pacs008 != null ? pacs008.getPacs008Id() : null)
                .instructionId(pacs008 != null ? pacs008.getInstructionId() : null)
                .txId(pacs008 != null ? pacs008.getTxId() : null)
                .uetr(pacs008 != null ? pacs008.getUetr() : null)
                .endToEndId(pacs008 != null ? pacs008.getEndToEndId() : null)
                .amount(transaction.getAmount())
                .currency(pacs008 != null ? pacs008.getCurrency() : null)
                .transactionStatus(transaction.getTransactionStatus())
                .createdDate(transaction.getTransactionDateTime())
                .build();
    }

    private void validateRequest(PaymentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.getBeneficiaryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "beneficiaryId is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be greater than 0");
        }
    }

    private void validateBeneficiaryStatus(Beneficiary beneficiary) {
        String status = beneficiary.getStatus();
        if (!"APPROVED".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Beneficiary must be approved before payment");
        }
    }

    private Account selectSenderAccount(Long userId) {
        List<Account> accounts = accountRepository.findByUserUserId(userId);
        if (accounts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender account not found");
        }

        return accounts.stream()
                .filter(account -> "ACTIVE".equalsIgnoreCase(account.getStatus()))
                .min(Comparator.comparing(Account::getAccountId))
                .orElseGet(() -> accounts.stream()
                        .min(Comparator.comparing(Account::getAccountId))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender account not found")));
    }

    private String resolveCurrency(Account account) {
        String currency = account.getCurrency();
        return currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCountry(String countryCode) {
        String country = requireSnapshotValue(countryCode, "Country code is required");
        return country.trim().toUpperCase(Locale.ROOT);
    }

    private String createPendingPaymentKey(Long userId, Long beneficiaryId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest((userId + ":" + beneficiaryId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private String requireSnapshotValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
