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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
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

        Beneficiary beneficiary = beneficiaryRepository.findById(request.getBeneficiaryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
        validateBeneficiaryStatus(beneficiary);

        User sender = beneficiary.getUser();
        if (sender == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Beneficiary is not linked to a user");
        }

        Account senderAccount = selectSenderAccount(sender.getUserId());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        String transferId = idGenerationService.generateTransferId();
        String paymentTransactionId = idGenerationService.generatePaymentTransactionId();
        String bankTransactionId = idGenerationService.generateBankTransactionId();
        String messageId = idGenerationService.generateMessageId();
        String fromMmbId = idGenerationService.generateMemberId();
        String toMmbId = idGenerationService.generateMemberId();
        String instgAgtMmbId = idGenerationService.generateMemberId();
        String instdAgtMmbId = idGenerationService.generateMemberId();
        String dbtrAgtMmbId = instgAgtMmbId;
        String cdtrAgtMmbId = instdAgtMmbId;

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .transferId(transferId)
                .paymentTransactionId(paymentTransactionId)
                .bankTransactionId(bankTransactionId)
                .accountNumber(senderAccount.getAccountNumber())
                .amount(request.getAmount())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .beneficiaryAccountNumber(beneficiary.getAccountNumber())
                .beneficiaryRoutingNumber(beneficiary.getRoutingNumber())
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
                .fromMmbId(fromMmbId)
                .toMmbId(toMmbId)
                .instgAgtMmbId(instgAgtMmbId)
                .instdAgtMmbId(instdAgtMmbId)
                .dbtrAgtMmbId(dbtrAgtMmbId)
                .cdtrAgtMmbId(cdtrAgtMmbId)
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

        return PaymentResponse.builder()
                .transactionId(transaction.getTransactionId())
                .transferId(transaction.getTransferId())
                .paymentTransactionId(transaction.getPaymentTransactionId())
                .bankTransactionId(transaction.getBankTransactionId())
                .messageId(messageHeader.getMessageId())
                .businessMessageId(messageHeader.getBusinessMessageId())
                .pacs008Id(pacs008.getPacs008Id())
                .instructionId(pacs008.getInstructionId())
                .txId(pacs008.getTxId())
                .uetr(pacs008.getUetr())
                .endToEndId(pacs008.getEndToEndId())
                .fromMmbId(pacs008.getFromMmbId())
                .toMmbId(pacs008.getToMmbId())
                .instgAgtMmbId(pacs008.getInstgAgtMmbId())
                .instdAgtMmbId(pacs008.getInstdAgtMmbId())
                .dbtrAgtMmbId(pacs008.getDbtrAgtMmbId())
                .cdtrAgtMmbId(pacs008.getCdtrAgtMmbId())
                .amount(transaction.getAmount())
                .currency(pacs008.getCurrency())
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

    private String requireSnapshotValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
