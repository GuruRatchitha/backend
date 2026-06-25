package com.bank.fedwire.service;

import com.bank.fedwire.dto.TransactionResponse;
import com.bank.fedwire.dto.EmployeeTransactionQueueResponse;
import com.bank.fedwire.dto.TransactionDetailResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.repository.MessageHeaderRepository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final String CHANNEL_FEDWIRE = "FEDWIRE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ON_HOLD = "ON_HOLD";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_APPROVED = "APPROVED";

    private final TransactionRepository transactionRepository;
    private final MessageHeaderRepository messageHeaderRepository;
    private final PACS008Repository pacs008Repository;
    private final Pacs008XmlGeneratorService pacs008XmlGeneratorService;

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
    @Transactional
    public TransactionDetailResponse holdTransaction(Long transactionId) {
        return updateTransactionStatus(transactionId, STATUS_ON_HOLD);
    }

    @Override
    @Transactional
    public TransactionDetailResponse rejectTransaction(Long transactionId) {
        return updateTransactionStatus(transactionId, STATUS_REJECTED);
    }

    @Override
    @Transactional
    public String approveTransaction(Long transactionId) {
        updateTransactionStatus(transactionId, STATUS_APPROVED);
        return pacs008XmlGeneratorService.generateXml(transactionId);
    }

    private TransactionDetailResponse updateTransactionStatus(Long transactionId, String status) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (status.equalsIgnoreCase(transaction.getTransactionStatus())) {
            return toEmployeeTransactionDetailResponse(transaction);
        }

        transaction.setTransactionStatus(status);
        transaction.setPendingPaymentKey(null);
        transactionRepository.save(transaction);

        messageHeaderRepository.findByTransactionId(transactionId)
                .ifPresent(messageHeader -> updateMessageHeaderStatus(messageHeader, status));

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
        Account account = transaction.getAccount();
        String senderName = account != null && account.getUser() != null ? account.getUser().getUserName() : null;

        return TransactionDetailResponse.builder()
                .transactionId(transaction.getTransactionId())
                .senderDetails(TransactionDetailResponse.PartyDetails.builder()
                        .name(senderName)
                        .accountNumber(transaction.getAccountNumber())
                        .routingNumber(null)
                        .bankName(null)
                        .build())
                .receiverDetails(TransactionDetailResponse.PartyDetails.builder()
                        .name(transaction.getBeneficiaryName())
                        .accountNumber(transaction.getBeneficiaryAccountNumber())
                        .routingNumber(transaction.getBeneficiaryRoutingNumber())
                        .bankName(null)
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

        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private void updateMessageHeaderStatus(MessageHeader messageHeader, String status) {
        messageHeader.setMessageStatus(status);
        messageHeaderRepository.save(messageHeader);
    }
}
