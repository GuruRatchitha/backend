package com.bank.fedwire.service;

import com.bank.fedwire.dto.AccountSummary;
import com.bank.fedwire.dto.DashboardSummaryResponse;
import com.bank.fedwire.dto.TransactionPageResponse;
import com.bank.fedwire.dto.TransactionResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.Transaction;
import com.bank.fedwire.entity.TransactionStatus;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        List<Account> accounts = accountRepository.findByUserUserId(userId);

        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .filter(balance -> balance != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AccountSummary> accountSummaries = accounts.stream()
                .map(this::toAccountSummary)
                .toList();

        return DashboardSummaryResponse.builder()
                .totalBalance(totalBalance)
                .accountCount(accountRepository.countByUserUserId(userId))
                .completedTransactions(transactionRepository.countByStatusAndUserId(TransactionStatus.COMPLETED, userId))
                .pendingTransactions(transactionRepository.countByStatusAndUserId(TransactionStatus.PENDING, userId))
                .accounts(accountSummaries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionPageResponse getTransactions(Long userId, String limit, int page, int size) {
        if ("5".equals(limit)) {
            List<TransactionResponse> transactions = transactionRepository
                    .findTop5ByAccountUserUserIdOrderByTransactionDateTimeDesc(userId).stream()
                    .map(this::toTransactionResponse)
                    .toList();

            return TransactionPageResponse.builder()
                    .transactions(transactions)
                    .page(0)
                    .size(5)
                    .totalElements(transactions.size())
                    .totalPages(transactions.isEmpty() ? 0 : 1)
                    .build();
        }

        if (!"all".equalsIgnoreCase(limit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be 5 or all");
        }

        PageRequest pageRequest = buildPageRequest(page, size);
        Page<Transaction> transactions = transactionRepository
                .findByAccountUserUserIdOrderByTransactionDateTimeDesc(userId, pageRequest);

        return TransactionPageResponse.builder()
                .transactions(transactions.getContent().stream()
                        .map(this::toTransactionResponse)
                        .toList())
                .page(transactions.getNumber())
                .size(transactions.getSize())
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .build();
    }

    private AccountSummary toAccountSummary(Account account) {
        return AccountSummary.builder()
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .monthlyChange(0.0)
                .build();
    }

    private PageRequest buildPageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize);
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
}
