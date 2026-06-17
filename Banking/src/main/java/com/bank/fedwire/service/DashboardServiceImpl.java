package com.bank.fedwire.service;

import com.bank.fedwire.dto.AccountSummary;
import com.bank.fedwire.dto.DashboardSummaryResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.TransactionStatus;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private AccountSummary toAccountSummary(Account account) {
        return AccountSummary.builder()
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .monthlyChange(0.0)
                .build();
    }
}
