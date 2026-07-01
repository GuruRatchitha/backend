package com.bank.fedwire.service;

import com.bank.fedwire.dto.SettlementAccountResponse;
import com.bank.fedwire.dto.SettlementTransactionResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.PACS002;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.MessageHeader;
import com.bank.fedwire.entity.SettlementTransaction;
import com.bank.fedwire.entity.SettlementTransactionStatus;
import com.bank.fedwire.entity.SettlementTransactionType;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.entity.TransactionStatus;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.MessageHeaderRepository;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.repository.SettlementTransactionRepository;
import com.bank.fedwire.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementTransactionServiceImpl implements SettlementTransactionService {

    private static final String SETTLEMENT_ACCOUNT_TYPE = "SETTLEMENT";
    private static final String STATUS_APPROVED = TransactionStatus.APPROVED.name();
    private static final String STATUS_PROCESSING = TransactionStatus.PROCESSING.name();
    private static final String STATUS_WAITING = TransactionStatus.WAITING_FOR_PACS002.name();
    private static final String STATUS_COMPLETED = TransactionStatus.COMPLETED.name();
    private static final String STATUS_RETURN = TransactionStatus.RETURN.name();
    private static final String STATUS_REVERTED = TransactionStatus.REVERTED.name();

    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final MessageHeaderRepository messageHeaderRepository;
    private final PACS008Repository pacs008Repository;
    private final SettlementTransactionRepository settlementTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final Pacs008XmlGeneratorService pacs008XmlGeneratorService;
    private final SnsPublisherService snsPublisherService;

    @Override
    @Transactional(readOnly = true)
    public SettlementAccountResponse getSettlementAccount() {
        Account account = resolveSettlementAccount();
        log.debug("Loaded settlement account {}", account.getAccountNumber());
        return toSettlementAccountResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementTransactionResponse> getAllSettlementTransactions() {
        return getSettlementTransactions(null, null, null, null,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementTransactionResponse> getSettlementTransactionsByPaymentId(Long paymentId) {
        if (paymentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentId is required");
        }
        return getSettlementTransactions(paymentId, null, null, null,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementTransactionResponse> getSettlementTransactionsByStatus(String status) {
        return getSettlementTransactions(null, null, status, null,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementTransactionResponse> getSettlementTransactionsByType(String type) {
        return getSettlementTransactions(null, null, null, type,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SettlementTransactionResponse> getSettlementTransactions(
            Long paymentId,
            String accountNumber,
            String status,
            String transactionType,
            Pageable pageable) {
        Pageable effectivePageable = pageable != null && !pageable.isUnpaged()
                ? pageable
                : PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<SettlementTransaction> specification = buildSpecification(paymentId, accountNumber, status, transactionType);
        Page<SettlementTransaction> transactionPage = settlementTransactionRepository.findAll(specification, effectivePageable);
        SettlementResponseContext responseContext = buildResponseContext(transactionPage.getContent());
        List<SettlementTransactionResponse> responses = transactionPage.getContent().stream()
                .map(settlementTransaction -> toResponse(settlementTransaction, responseContext))
                .toList();
        Page<SettlementTransactionResponse> result = new PageImpl<>(
                responses,
                effectivePageable,
                transactionPage.getTotalElements());
        log.debug("Loaded {} settlement transactions for paymentId={}, accountNumber={}, status={}, type={}",
                result.getNumberOfElements(), paymentId, accountNumber, status, transactionType);
        return result;
    }

    @Override
    @Transactional
    public void processApproval(Transaction transaction, PACS008 pacs008) {
        requireTransaction(transaction);
        if (pacs008 == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PACS008 record is required");
        }
        Transaction lockedTransaction = transactionRepository.findByTransactionIdForUpdate(transaction.getTransactionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found for transactionId " + transaction.getTransactionId()));
        if (!STATUS_APPROVED.equalsIgnoreCase(lockedTransaction.getTransactionStatus())) {
            lockedTransaction.setTransactionStatus(STATUS_APPROVED);
        }
        transactionRepository.saveAndFlush(lockedTransaction);

        MessageHeader messageHeader = messageHeaderRepository.findTopByTransactionIdOrderByCreatedDateDesc(lockedTransaction.getTransactionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Message header not found for transactionId " + lockedTransaction.getTransactionId()));
        messageHeader.setMessageStatus(STATUS_APPROVED);
        messageHeaderRepository.saveAndFlush(messageHeader);

        Account senderAccount = lockSenderAccount(lockedTransaction.getAccountNumber());
        Account settlementAccount = lockSettlementAccount();
        BigDecimal amount = requireAmount(lockedTransaction.getAmount());

        pacs008XmlGeneratorService.generateXml(lockedTransaction.getTransactionId());
        snsPublisherService.publishIfNeeded(lockedTransaction.getTransactionId());

        boolean alreadyDebited = settlementTransactionRepository.existsByPaymentIdAndTransactionTypeAndStatus(
                lockedTransaction.getTransactionId(),
                SettlementTransactionType.DEBIT_TO_SETTLEMENT,
                SettlementTransactionStatus.SUCCESS);
        if (alreadyDebited) {
            lockedTransaction.setTransactionStatus(STATUS_PROCESSING);
            lockedTransaction.setPendingPaymentKey(null);
            transactionRepository.saveAndFlush(lockedTransaction);
            messageHeader.setMessageStatus(STATUS_PROCESSING);
            messageHeaderRepository.saveAndFlush(messageHeader);
            log.info("Approval settlement already exists for paymentId={}; leaving balances unchanged",
                    lockedTransaction.getTransactionId());
            return;
        }

        debit(senderAccount, amount);
        credit(settlementAccount, amount);
        accountRepository.save(senderAccount);
        accountRepository.save(settlementAccount);

        SettlementTransaction settlementDebit = settlementTransactionRepository.save(
                SettlementTransaction.builder()
                        .paymentId(lockedTransaction.getTransactionId())
                        .senderAccount(senderAccount.getAccountNumber())
                        .beneficiaryAccount(lockedTransaction.getBeneficiaryAccountNumber())
                        .settlementAccount(settlementAccount.getAccountNumber())
                        .amount(amount)
                        .transactionType(SettlementTransactionType.DEBIT_TO_SETTLEMENT)
                        .status(SettlementTransactionStatus.SUCCESS)
                        .pacs008MessageId(pacs008.getMessageId())
                        .build());

        settlementDebit.setPacs008MessageId(pacs008.getMessageId());
        settlementTransactionRepository.save(settlementDebit);
        lockedTransaction.setTransactionStatus(STATUS_PROCESSING);
        lockedTransaction.setPendingPaymentKey(null);
        transactionRepository.saveAndFlush(lockedTransaction);
        messageHeader.setMessageStatus(STATUS_PROCESSING);
        messageHeaderRepository.saveAndFlush(messageHeader);

        log.info("Approved paymentId={} and moved funds to settlement account {}", lockedTransaction.getTransactionId(),
                settlementAccount.getAccountNumber());
    }

    @Override
    @Transactional
    public void processPacs002(Transaction transaction, PACS002 pacs002) {
        requireTransaction(transaction);
        if (pacs002 == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PACS002 record is required");
        }
        log.info("Processing PACS002 settlement for transactionId={}, pacs002Status={}, originalMessageId={}, messageId={}, transferId={}",
                transaction.getTransactionId(), pacs002.getTransactionStatus(), pacs002.getOriginalMessageId(),
                pacs002.getMessageId(), pacs002.getTransferId());
        Transaction lockedTransaction = transactionRepository.findByTransactionIdForUpdate(transaction.getTransactionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found for transactionId " + transaction.getTransactionId()));
        String status = normalizePacs002Status(pacs002.getTransactionStatus());
        if (!STATUS_APPROVED.equalsIgnoreCase(lockedTransaction.getTransactionStatus())
                && !STATUS_PROCESSING.equalsIgnoreCase(lockedTransaction.getTransactionStatus())
                && !STATUS_WAITING.equalsIgnoreCase(lockedTransaction.getTransactionStatus())
                && !STATUS_COMPLETED.equalsIgnoreCase(lockedTransaction.getTransactionStatus())
                && !STATUS_RETURN.equalsIgnoreCase(lockedTransaction.getTransactionStatus())
                && !STATUS_REVERTED.equalsIgnoreCase(lockedTransaction.getTransactionStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PACS002 can only be processed after approval");
        }

        SettlementTransaction settlementDebit = settlementTransactionRepository
                .findTopByPaymentIdAndTransactionTypeOrderByCreatedAtDesc(
                        lockedTransaction.getTransactionId(),
                        SettlementTransactionType.DEBIT_TO_SETTLEMENT)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Settlement transaction not found for paymentId " + lockedTransaction.getTransactionId()));

        log.info("Applying PACS002 status {} for transactionId={}, settlementAccount={}, amount={}",
                status, lockedTransaction.getTransactionId(), settlementDebit.getSettlementAccount(), settlementDebit.getAmount());
        if ("ACSC".equals(status)) {
            boolean alreadyCredited = settlementTransactionRepository.existsByPaymentIdAndTransactionTypeAndStatus(
                    lockedTransaction.getTransactionId(),
                    SettlementTransactionType.CREDIT_TO_BENEFICIARY,
                    SettlementTransactionStatus.SUCCESS);
            if (alreadyCredited) {
                settlementDebit.setPacs002Status(status);
                settlementTransactionRepository.save(settlementDebit);
                lockedTransaction.setTransactionStatus(STATUS_COMPLETED);
                updateTransactionAndHeader(lockedTransaction);
                log.info("PACS002 ACSC already settled for paymentId={}; duplicate beneficiary settlement skipped",
                        lockedTransaction.getTransactionId());
                return;
            }

            Account settlementAccount = lockAccount(settlementDebit.getSettlementAccount());
            BigDecimal amount = requireAmount(settlementDebit.getAmount());
            debit(settlementAccount, amount);
            accountRepository.save(settlementAccount);
            Beneficiary beneficiary = resolveBeneficiary(
                    settlementDebit.getBeneficiaryAccount(),
                    lockedTransaction.getBeneficiaryRoutingNumber());
            log.info("Resolved beneficiary record beneficiaryId={}, accountNumber={}, routingNumber={}, status={} for transactionId={}",
                    beneficiary.getBeneficiaryId(), beneficiary.getAccountNumber(), beneficiary.getRoutingNumber(),
                    beneficiary.getStatus(), lockedTransaction.getTransactionId());
            accountRepository.findByAccountNumberForUpdate(beneficiary.getAccountNumber())
                    .ifPresent(beneficiaryAccount -> {
                        credit(beneficiaryAccount, amount);
                        accountRepository.save(beneficiaryAccount);
                    });

            settlementTransactionRepository.save(SettlementTransaction.builder()
                    .paymentId(lockedTransaction.getTransactionId())
                    .senderAccount(settlementDebit.getSenderAccount())
                    .beneficiaryAccount(settlementDebit.getBeneficiaryAccount())
                    .settlementAccount(settlementDebit.getSettlementAccount())
                    .amount(amount)
                    .transactionType(SettlementTransactionType.CREDIT_TO_BENEFICIARY)
                    .status(SettlementTransactionStatus.SUCCESS)
                    .pacs008MessageId(settlementDebit.getPacs008MessageId())
                    .pacs002Status(status)
                    .build());

            settlementDebit.setPacs002Status(status);
            settlementTransactionRepository.save(settlementDebit);
            lockedTransaction.setTransactionStatus(STATUS_COMPLETED);
        } else if ("RJCT".equals(status)) {
            settlementDebit.setPacs002Status(status);
            settlementTransactionRepository.save(settlementDebit);
            if (!STATUS_REVERTED.equalsIgnoreCase(lockedTransaction.getTransactionStatus())) {
                lockedTransaction.setTransactionStatus(STATUS_RETURN);
            }
        } else if ("ACCP".equals(status) || "ACSP".equals(status)) {
            settlementDebit.setPacs002Status(status);
            settlementTransactionRepository.save(settlementDebit);
            lockedTransaction.setTransactionStatus(STATUS_PROCESSING);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported PACS002 status " + pacs002.getTransactionStatus());
        }

        updateTransactionAndHeader(lockedTransaction);
        log.info("Processed PACS002 for paymentId={} with status={}", lockedTransaction.getTransactionId(), status);
    }

    @Override
    @Transactional
    public void revertToSender(Long transactionId) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId is required");
        }
        Transaction lockedTransaction = transactionRepository.findByTransactionIdForUpdate(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (STATUS_REVERTED.equalsIgnoreCase(lockedTransaction.getTransactionStatus())) {
            return;
        }
        if (!STATUS_RETURN.equalsIgnoreCase(lockedTransaction.getTransactionStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transaction can be reverted only from RETURN status");
        }

        SettlementTransaction settlementDebit = settlementTransactionRepository
                .findTopByPaymentIdAndTransactionTypeOrderByCreatedAtDesc(
                        lockedTransaction.getTransactionId(),
                        SettlementTransactionType.DEBIT_TO_SETTLEMENT)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Settlement transaction not found for paymentId " + lockedTransaction.getTransactionId()));

        boolean alreadyReverted = settlementTransactionRepository.existsByPaymentIdAndTransactionTypeAndStatus(
                lockedTransaction.getTransactionId(),
                SettlementTransactionType.RETURN_TO_SENDER,
                SettlementTransactionStatus.SUCCESS);
        if (!alreadyReverted) {
            BigDecimal amount = requireAmount(settlementDebit.getAmount());
            Account settlementAccount = lockAccount(settlementDebit.getSettlementAccount());
            Account senderAccount = lockAccount(settlementDebit.getSenderAccount());
            debit(settlementAccount, amount);
            credit(senderAccount, amount);
            accountRepository.save(settlementAccount);
            accountRepository.save(senderAccount);

            settlementTransactionRepository.save(SettlementTransaction.builder()
                    .paymentId(lockedTransaction.getTransactionId())
                    .senderAccount(settlementDebit.getSenderAccount())
                    .beneficiaryAccount(settlementDebit.getBeneficiaryAccount())
                    .settlementAccount(settlementDebit.getSettlementAccount())
                    .amount(amount)
                    .transactionType(SettlementTransactionType.RETURN_TO_SENDER)
                    .status(SettlementTransactionStatus.SUCCESS)
                    .pacs008MessageId(settlementDebit.getPacs008MessageId())
                    .pacs002Status(settlementDebit.getPacs002Status())
                    .build());
        }

        lockedTransaction.setTransactionStatus(STATUS_REVERTED);
        lockedTransaction.setPendingPaymentKey(null);
        updateTransactionAndHeader(lockedTransaction);
    }

    private void updateTransactionAndHeader(Transaction lockedTransaction) {
        log.info("Updating transaction status for transactionId={} to {}", lockedTransaction.getTransactionId(),
                lockedTransaction.getTransactionStatus());
        transactionRepository.saveAndFlush(lockedTransaction);
        MessageHeader messageHeader = messageHeaderRepository.findTopByTransactionIdOrderByCreatedDateDesc(lockedTransaction.getTransactionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Message header not found for transactionId " + lockedTransaction.getTransactionId()));
        messageHeader.setMessageStatus(lockedTransaction.getTransactionStatus());
        messageHeaderRepository.saveAndFlush(messageHeader);
    }

    private Account resolveSettlementAccount() {
        return accountRepository.findByAccountType(SETTLEMENT_ACCOUNT_TYPE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Settlement account not found in account table"));
    }

    private Account lockSettlementAccount() {
        return accountRepository.findByAccountTypeForUpdate(SETTLEMENT_ACCOUNT_TYPE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Settlement account not found in account table"));
    }

    private Account lockSenderAccount(String accountNumber) {
        return lockAccount(accountNumber);
    }

    private Account lockAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountNumber is required");
        }
        return accountRepository.findByAccountNumberForUpdate(accountNumber.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found for account number " + accountNumber));
    }

    private Beneficiary resolveBeneficiary(String accountNumber, String routingNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "beneficiaryAccountNumber is required");
        }
        if (routingNumber == null || routingNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "beneficiaryRoutingNumber is required");
        }

        Beneficiary beneficiary = beneficiaryRepository.findByAccountNumberAndRoutingNumber(
                        accountNumber.trim(), routingNumber.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Beneficiary not found for account number " + accountNumber + " and routing number " + routingNumber));

        String status = beneficiary.getStatus();
        if (status != null && !status.isBlank()
                && !"APPROVED".equalsIgnoreCase(status)
                && !"ACTIVE".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Beneficiary is not active for account number " + accountNumber);
        }

        return beneficiary;
    }

    private void debit(Account account, BigDecimal amount) {
        BigDecimal currentBalance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        if (currentBalance.compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient balance in account " + account.getAccountNumber());
        }
        account.setBalance(currentBalance.subtract(amount));
        account.setUpdatedDate(LocalDateTime.now(ZoneOffset.UTC));
    }

    private void credit(Account account, BigDecimal amount) {
        BigDecimal currentBalance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        account.setBalance(currentBalance.add(amount));
        account.setUpdatedDate(LocalDateTime.now(ZoneOffset.UTC));
    }

    private BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be greater than 0");
        }
        return amount;
    }

    private void requireTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction is required");
        }
    }

    private SettlementAccountResponse toSettlementAccountResponse(Account account) {
        BigDecimal creditedToSettlement = nullToZero(settlementTransactionRepository.sumAmountByTransactionType(
                SettlementTransactionType.DEBIT_TO_SETTLEMENT));
        BigDecimal debitedToBeneficiary = nullToZero(settlementTransactionRepository.sumAmountByTransactionType(
                SettlementTransactionType.CREDIT_TO_BENEFICIARY));
        BigDecimal returnedToSender = nullToZero(settlementTransactionRepository.sumAmountByTransactionType(
                SettlementTransactionType.RETURN_TO_SENDER));
        BigDecimal revertAmount = nullToZero(settlementTransactionRepository.sumAmountWaitingToRevert());
        BigDecimal currentBalance = creditedToSettlement
                .subtract(debitedToBeneficiary)
                .subtract(returnedToSender)
                .subtract(revertAmount);

        return new SettlementAccountResponse(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getAccountNumber(),
                account.getAccountName(),
                account.getAccountType(),
                account.getCurrency(),
                account.getBalance(),
                currentBalance,
                revertAmount,
                revertAmount,
                account.getStatus(),
                account.getCreatedDate(),
                account.getUpdatedDate(),
                List.of());
    }

    private SettlementTransactionResponse toResponse(SettlementTransaction settlementTransaction) {
        return toResponse(settlementTransaction, buildResponseContext(List.of(settlementTransaction)));
    }

    private SettlementTransactionResponse toResponse(
            SettlementTransaction settlementTransaction,
            SettlementResponseContext responseContext) {
        PACS008 pacs008 = responseContext.pacs008ByTransactionId().get(settlementTransaction.getPaymentId());
        Account senderAccount = responseContext.accountsByNumber().get(settlementTransaction.getSenderAccount());
        Account receiverAccount = responseContext.accountsByNumber().get(settlementTransaction.getBeneficiaryAccount());

        return new SettlementTransactionResponse(
                settlementTransaction.getSettlementTransactionId(),
                settlementTransaction.getPaymentId(),
                resolveHistoryAccountNumber(settlementTransaction),
                settlementTransaction.getSenderAccount(),
                firstNonBlank(senderAccount != null ? senderAccount.getAccountName() : null,
                        pacs008 != null ? pacs008.getDebtorName() : null),
                senderAccount != null ? senderAccount.getAccountType() : null,
                settlementTransaction.getBeneficiaryAccount(),
                settlementTransaction.getBeneficiaryAccount(),
                firstNonBlank(
                        receiverAccount != null ? receiverAccount.getAccountName() : null,
                        pacs008 != null ? pacs008.getCreditorName() : null),
                receiverAccount != null ? receiverAccount.getAccountType() : null,
                settlementTransaction.getSenderAccount(),
                settlementTransaction.getBeneficiaryAccount(),
                settlementTransaction.getSettlementAccount(),
                displayAmount(settlementTransaction),
                resolveHistoryStatus(settlementTransaction.getTransactionType()),
                settlementTransaction.getPacs008MessageId(),
                pacs008 != null ? pacs008.getUetr() : null,
                settlementTransaction.getPacs002Status(),
                settlementTransaction.getCreatedAt(),
                settlementTransaction.getCreatedAt(),
                settlementTransaction.getUpdatedAt());
    }

    private SettlementResponseContext buildResponseContext(List<SettlementTransaction> settlementTransactions) {
        if (settlementTransactions == null || settlementTransactions.isEmpty()) {
            return new SettlementResponseContext(Map.of(), Map.of());
        }

        Set<Long> paymentIds = settlementTransactions.stream()
                .map(SettlementTransaction::getPaymentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PACS008> pacs008ByTransactionId = paymentIds.isEmpty()
                ? Map.of()
                : pacs008Repository.findByTransactionIdIn(paymentIds).stream()
                .collect(Collectors.toMap(
                        PACS008::getTransactionId,
                        Function.identity(),
                        this::newerPacs008));

        Set<String> accountNumbers = settlementTransactions.stream()
                .flatMap(settlementTransaction -> Stream.of(
                        settlementTransaction.getSenderAccount(),
                        settlementTransaction.getBeneficiaryAccount()))
                .filter(accountNumber -> accountNumber != null && !accountNumber.isBlank())
                .collect(Collectors.toSet());
        Map<String, Account> accountsByNumber = accountNumbers.isEmpty()
                ? Map.of()
                : accountRepository.findByAccountNumberIn(accountNumbers).stream()
                .collect(Collectors.toMap(Account::getAccountNumber, Function.identity(), (existing, duplicate) -> existing));

        return new SettlementResponseContext(pacs008ByTransactionId, accountsByNumber);
    }

    private PACS008 newerPacs008(PACS008 existing, PACS008 candidate) {
        if (existing.getCreatedDate() == null) {
            return candidate;
        }
        if (candidate.getCreatedDate() == null) {
            return existing;
        }
        return candidate.getCreatedDate().isAfter(existing.getCreatedDate()) ? candidate : existing;
    }

    private record SettlementResponseContext(
            Map<Long, PACS008> pacs008ByTransactionId,
            Map<String, Account> accountsByNumber) {
    }

    private String resolveHistoryAccountNumber(SettlementTransaction settlementTransaction) {
        if (settlementTransaction.getTransactionType() == null) {
            return null;
        }
        return switch (settlementTransaction.getTransactionType()) {
            case DEBIT_TO_SETTLEMENT -> settlementTransaction.getSenderAccount();
            case CREDIT_TO_BENEFICIARY -> settlementTransaction.getBeneficiaryAccount();
            case RETURN_TO_SENDER -> settlementTransaction.getSenderAccount();
        };
    }

    private String resolveHistoryStatus(SettlementTransactionType transactionType) {
        return switch (transactionType) {
            case DEBIT_TO_SETTLEMENT -> "Credited";
            case CREDIT_TO_BENEFICIARY -> "Debited";
            case RETURN_TO_SENDER -> "Returned";
        };
    }

    private BigDecimal displayAmount(SettlementTransaction settlementTransaction) {
        BigDecimal amount = nullToZero(settlementTransaction.getAmount());
        SettlementTransactionType transactionType = settlementTransaction.getTransactionType();
        if (transactionType == SettlementTransactionType.CREDIT_TO_BENEFICIARY
                || transactionType == SettlementTransactionType.RETURN_TO_SENDER) {
            return amount.abs().negate();
        }
        return amount;
    }

    private Specification<SettlementTransaction> buildSpecification(
            Long paymentId,
            String accountNumber,
            String status,
            String transactionType) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (paymentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentId"), paymentId));
            }

            if (accountNumber != null && !accountNumber.isBlank()) {
                String normalizedAccountNumber = accountNumber.trim();
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("senderAccount"), normalizedAccountNumber),
                        criteriaBuilder.equal(root.get("beneficiaryAccount"), normalizedAccountNumber),
                        criteriaBuilder.equal(root.get("settlementAccount"), normalizedAccountNumber)));
            }

            if (status != null && !status.isBlank()) {
                List<SettlementTransactionType> historyStatusTypes = parseHistoryStatusTypes(status);
                if (historyStatusTypes.isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), parseStatus(status)));
                } else {
                    predicates.add(root.get("transactionType").in(historyStatusTypes));
                }
            }

            if (transactionType != null && !transactionType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("transactionType"), parseType(transactionType)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<SettlementTransactionType> parseHistoryStatusTypes(String status) {
        String normalized = normalizeHistoryStatus(status);
        if ("credited".equals(normalized) || "credited to settlement".equals(normalized)) {
            return List.of(SettlementTransactionType.DEBIT_TO_SETTLEMENT);
        }
        if ("debited".equals(normalized) || "debited from settlement".equals(normalized)) {
            return List.of(SettlementTransactionType.CREDIT_TO_BENEFICIARY);
        }
        if ("returned".equals(normalized)) {
            return List.of(SettlementTransactionType.RETURN_TO_SENDER);
        }
        return List.of();
    }

    private String normalizeHistoryStatus(String status) {
        return status.trim()
                .replaceAll("(?i)\\bamount\\b", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private SettlementTransactionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        try {
            return SettlementTransactionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported settlement status " + status, ex);
        }
    }

    private SettlementTransactionType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
        }
        try {
            return SettlementTransactionType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported settlement type " + type, ex);
        }
    }

    private String normalizePacs002Status(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PACS002 status is required");
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
