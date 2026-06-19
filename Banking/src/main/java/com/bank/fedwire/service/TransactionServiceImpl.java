package com.bank.fedwire.service;

import com.bank.fedwire.dto.TransactionResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.Transaction;
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

    private final TransactionRepository transactionRepository;

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
