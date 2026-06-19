package com.bank.fedwire.service;

import com.bank.fedwire.dto.TransactionResponse;

import java.util.List;

public interface TransactionService {

    List<TransactionResponse> getTransactions(Long userId, Integer limit);
}
