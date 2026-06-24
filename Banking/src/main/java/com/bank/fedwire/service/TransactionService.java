package com.bank.fedwire.service;

import com.bank.fedwire.dto.TransactionResponse;
import com.bank.fedwire.dto.EmployeeTransactionQueueResponse;
import com.bank.fedwire.dto.TransactionDetailResponse;

import java.util.List;

public interface TransactionService {

    List<TransactionResponse> getTransactions(Long userId, Integer limit);

    List<EmployeeTransactionQueueResponse> getEmployeeTransactionQueue();

    TransactionDetailResponse getEmployeeTransactionDetails(Long transactionId);

    String getEmployeeTransactionPacs008Xml(Long transactionId);

    TransactionDetailResponse holdTransaction(Long transactionId);

    TransactionDetailResponse rejectTransaction(Long transactionId);

    String approveTransaction(Long transactionId);
}
