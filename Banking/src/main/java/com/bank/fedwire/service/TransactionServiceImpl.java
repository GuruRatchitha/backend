package com.bank.fedwire.service;

import com.bank.fedwire.dto.EmployeeTransactionQueueResponse;
import com.bank.fedwire.dto.TransactionDetailResponse;
import com.bank.fedwire.dto.TransactionResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.DashboardActivity;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.ADMI002Repository;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.DashboardActivityRepository;
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final String BANK_NAME = "ABC Bank";
    private static final String CHANNEL_FEDWIRE = "Fedwire";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ON_HOLD = "ON_HOLD";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_WAITING = "WAITING_FOR_PACS002";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final MessageHeaderRepository messageHeaderRepository;
    private final PACS002Repository pacs002Repository;
    private final ADMI002Repository admi002Repository;
    private final PACS008Repository pacs008Repository;
    private final SettlementTransactionService settlementTransactionService;
    private final DashboardActivityRepository dashboardActivityRepository;

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
        getTransaction(transactionId);

        return pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(transactionId)
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
    @Transactional(readOnly = true)
    public Optional<String> findEmployeeTransactionAdmi002Xml(Long transactionId) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }

        return admi002Repository.findTopByTransactionIdOrderByReceivedTimestampDesc(transactionId)
                .map(admi002 -> admi002.getXmlPayload())
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
        Transaction transaction = transactionRepository.findByTransactionIdForUpdate(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (isFinalStatus(transaction.getTransactionStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transaction is already in final status " + transaction.getTransactionStatus());
        }

        updateEmployeeTransactionStatus(transactionId, STATUS_APPROVED);
        Transaction approvedTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        PACS008 pacs008 = pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PACS008 record not found"));
        settlementTransactionService.processApproval(approvedTransaction, pacs008);
        return toEmployeeTransactionDetailResponse(transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found")));
    }

    private TransactionDetailResponse updateEmployeeTransactionStatus(Long transactionId, String status) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }

        Transaction transaction = transactionRepository.findByTransactionIdForUpdate(transactionId)
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
            logActivity("Transaction " + status,
                    "Transaction " + transaction.getTransactionId() + " was " + status.toLowerCase() + ".");

            MessageHeader messageHeader = messageHeaderRepository.findTopByTransactionIdOrderByCreatedDateDesc(transactionId)
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
        Transaction currentTransaction = transactionRepository.findById(transaction.getTransactionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
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
                .transactionId(currentTransaction.getTransactionId())
                .status(currentTransaction.getTransactionStatus())
                .senderDetails(TransactionDetailResponse.SenderDetails.builder()
                        .senderName(senderUser != null ? senderUser.getUserName() : null)
                        .senderAccountNumber(senderAccount.getAccountNumber())
                        .senderRoutingNumber(senderAccount.getRoutingNumber())
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
                        .transactionReference(String.valueOf(currentTransaction.getTransactionId()))
                        .amount(currentTransaction.getAmount())
                        .paymentDate(currentTransaction.getTransactionDateTime())
                        .status(currentTransaction.getTransactionStatus())
                        .channel(CHANNEL_FEDWIRE)
                        .build())
                .xmlMessages(TransactionDetailResponse.XmlMessages.builder()
                        .pacs008(findEmployeeTransactionPacs008Xml(currentTransaction.getTransactionId()).orElse(null))
                        .pacs002(findEmployeeTransactionPacs002Xml(currentTransaction.getTransactionId()).orElse(null))
                        .admi002(findEmployeeTransactionAdmi002Xml(currentTransaction.getTransactionId()).orElse(null))
                        .build())
                .build();
    }

    private Optional<String> findEmployeeTransactionPacs008Xml(Long transactionId) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }

        return pacs008Repository.findTopByTransactionIdOrderByCreatedDateDesc(transactionId)
                .map(PACS008::getXmlPayload)
                .filter(xml -> xml != null && !xml.isBlank());
    }

    private Transaction getTransaction(Long transactionId) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }

        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private boolean isFinalStatus(String status) {
        return STATUS_APPROVED.equalsIgnoreCase(status)
                || STATUS_COMPLETED.equalsIgnoreCase(status)
                || STATUS_REJECTED.equalsIgnoreCase(status)
                || STATUS_WAITING.equalsIgnoreCase(status)
                || STATUS_SUCCESS.equalsIgnoreCase(status);
    }

    private String normalizeCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private void logActivity(String activity, String description) {
        dashboardActivityRepository.save(DashboardActivity.builder()
                .activity(activity)
                .description(description)
                .employeeName("System")
                .build());
    }
}
