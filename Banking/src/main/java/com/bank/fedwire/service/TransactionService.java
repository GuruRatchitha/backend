package com.bank.fedwire.service;

import com.bank.fedwire.dto.TransactionResponse;
import com.bank.fedwire.dto.EmployeeTransactionQueueResponse;
import com.bank.fedwire.dto.TransactionDetailResponse;

import java.util.List;
import java.util.Optional;

public interface TransactionService {

    List<TransactionResponse> getTransactions(Long userId, Integer limit);

    List<EmployeeTransactionQueueResponse> getEmployeeTransactionQueue();

    TransactionDetailResponse getEmployeeTransactionDetails(Long transactionId);

    String getEmployeeTransactionPacs008Xml(Long transactionId);

    Optional<String> findEmployeeTransactionPacs002Xml(Long transactionId);

    Optional<String> findEmployeeTransactionAdmi002Xml(Long transactionId);

    TransactionDetailResponse holdTransaction(Long transactionId);

    TransactionDetailResponse rejectTransaction(Long transactionId);

    TransactionDetailResponse approveTransaction(Long transactionId);

    TransactionDetailResponse revertTransaction(Long transactionId);
}
