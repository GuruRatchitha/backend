package com.bank.fedwire.service;

import com.bank.fedwire.dto.TransactionResponse;
import com.bank.fedwire.dto.EmployeeTransactionQueueResponse;
import com.bank.fedwire.dto.TransactionDetailResponse;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.event.Pacs008ApprovedEvent;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.MessageHeaderRepository;
import com.bank.fedwire.repository.PACS002Repository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final String BANK_NAME = "ABC Bank";
    private static final String CHANNEL_FEDWIRE = "Fedwire";
    private static final String SENDER_ROUTING_NUMBER = "653060219";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ON_HOLD = "ON_HOLD";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_APPROVED = "APPROVED";

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final MessageHeaderRepository messageHeaderRepository;
    private final PACS002Repository pacs002Repository;
    private final PACS008Repository pacs008Repository;
    private final Pacs008XmlGeneratorService pacs008XmlGeneratorService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long userId, Integer limit) {
        if (limit == null) {
            return transactionRepository.findByAccountUserUserIdOrderByTransactionDateTimeDesc(userId).stream()
                    .map(this::toTransactionResponse)
                    .toList();
        }

        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be greater than 0");
        }

        return transactionRepository.findByAccountUserUserIdOrderByTransactionDateTimeDesc(
                        userId, PageRequest.of(0, limit)).stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeTransactionQueueResponse> getEmployeeTransactionQueue() {
        return transactionRepository.findEmployeeTransactionQueue();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponse getEmployeeTransactionDetails(Long transactionId) {
        Transaction transaction = getTransaction(transactionId);
        return toEmployeeTransactionDetailResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public String getEmployeeTransactionPacs008Xml(Long transactionId) {
        Transaction transaction = getTransaction(transactionId);
        String status = transaction.getTransactionStatus();

        if (STATUS_PENDING.equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Employee approval is required to generate PACS.008 XML");
        }
        if (STATUS_REJECTED.equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transaction rejected. PACS.008 XML was not generated.");
        }
        if (!STATUS_APPROVED.equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PACS.008 XML is not available for transaction status " + status);
        }

        return pacs008Repository.findByTransactionId(transactionId)
                .map(pacs008 -> {
                    if (pacs008.getXmlPayload() == null || pacs008.getXmlPayload().isBlank()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PACS008 XML not generated for transaction");
                    }
                    return pacs008.getXmlPayload();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PACS008 record not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findEmployeeTransactionPacs002Xml(Long transactionId) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }

        return pacs002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(transactionId)
                .map(pacs002 -> pacs002.getXmlPayload())
                .filter(xml -> xml != null && !xml.isBlank());
    }

    @Override
    @Transactional
    public TransactionDetailResponse holdTransaction(Long transactionId) {
        return updateEmployeeTransactionStatus(transactionId, STATUS_ON_HOLD);
    }

    @Override
    @Transactional
    public TransactionDetailResponse rejectTransaction(Long transactionId) {
        return updateEmployeeTransactionStatus(transactionId, STATUS_REJECTED);
    }

    @Override
    @Transactional
    public TransactionDetailResponse approveTransaction(Long transactionId) {
        TransactionDetailResponse response = updateEmployeeTransactionStatus(transactionId, STATUS_APPROVED);
        pacs008XmlGeneratorService.generateXml(transactionId);
        applicationEventPublisher.publishEvent(new Pacs008ApprovedEvent(transactionId));
        return response;
    }

    private TransactionDetailResponse updateEmployeeTransactionStatus(Long transactionId, String status) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        String currentStatus = transaction.getTransactionStatus();
        if (isFinalStatus(currentStatus) && !status.equalsIgnoreCase(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transaction is already in final status " + currentStatus);
        }

        if (!status.equalsIgnoreCase(currentStatus)) {
            transaction.setTransactionStatus(status);
            transaction.setPendingPaymentKey(null);
            transactionRepository.saveAndFlush(transaction);

            MessageHeader messageHeader = messageHeaderRepository.findByTransactionId(transactionId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Message header not found for transaction"));
            messageHeader.setMessageStatus(status);
            messageHeaderRepository.saveAndFlush(messageHeader);

        }

        return toEmployeeTransactionDetailResponse(transaction);
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        Account account = transaction.getAccount();

        return TransactionResponse.builder()
                .transactionReference(String.valueOf(transaction.getTransactionId()))
                .beneficiaryName(transaction.getBeneficiaryName())
                .amount(transaction.getAmount())
                .currency(account != null ? account.getCurrency() : null)
                .purpose(transaction.getRemarks())
                .transactionStatus(transaction.getTransactionStatus())
                .transferStatus(transaction.getTransactionStatus())
                .transactionDate(transaction.getTransactionDateTime())
                .accountNumber(transaction.getAccountNumber())
                .accountType(account != null ? account.getAccountType() : null)
                .build();
    }

    private TransactionDetailResponse toEmployeeTransactionDetailResponse(Transaction transaction) {
        Account senderAccount = accountRepository.findByAccountNumber(transaction.getAccountNumber())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sender account not found for account number " + transaction.getAccountNumber()));
        User senderUser = senderAccount.getUser();
        Beneficiary beneficiary = beneficiaryRepository.findByAccountNumberAndRoutingNumber(
                        transaction.getBeneficiaryAccountNumber(),
                        transaction.getBeneficiaryRoutingNumber())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Beneficiary not found for account number " + transaction.getBeneficiaryAccountNumber()));

        return TransactionDetailResponse.builder()
                .transactionId(transaction.getTransactionId())
                .senderDetails(TransactionDetailResponse.SenderDetails.builder()
                        .senderName(senderUser != null ? senderUser.getUserName() : null)
                        .senderAccountNumber(senderAccount.getAccountNumber())
                        .senderRoutingNumber(SENDER_ROUTING_NUMBER)
                        .senderBankName(BANK_NAME)
                        .senderCountry(normalizeCountry(senderUser != null ? senderUser.getCountryCode() : null))
                        .build())
                .receiverDetails(TransactionDetailResponse.ReceiverDetails.builder()
                        .receiverName(beneficiary.getBeneficiaryName())
                        .receiverAccountNumber(beneficiary.getAccountNumber())
                        .receiverRoutingNumber(beneficiary.getRoutingNumber())
                        .receiverBankName(BANK_NAME)
                        .receiverCountry(normalizeCountry(beneficiary.getCountryCode()))
                        .build())
                .paymentDetails(TransactionDetailResponse.PaymentDetails.builder()
                        .transactionReference(String.valueOf(transaction.getTransactionId()))
                        .amount(transaction.getAmount())
                        .paymentDate(transaction.getTransactionDateTime())
                        .status(transaction.getTransactionStatus())
                        .channel(CHANNEL_FEDWIRE)
                        .build())
                .build();
    }

    private Transaction getTransaction(Long transactionId) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }

        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private boolean isFinalStatus(String status) {
        return STATUS_APPROVED.equalsIgnoreCase(status) || STATUS_REJECTED.equalsIgnoreCase(status);
    }

    private String normalizeCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }
}
