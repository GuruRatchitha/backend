package com.bank.fedwire.service;

import com.bank.fedwire.dto.SettlementAccountResponse;
import com.bank.fedwire.dto.SettlementTransactionResponse;
import com.bank.fedwire.entity.PACS002;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SettlementTransactionService {

    SettlementAccountResponse getSettlementAccount();

    List<SettlementTransactionResponse> getAllSettlementTransactions();

    List<SettlementTransactionResponse> getSettlementTransactionsByPaymentId(Long paymentId);

    List<SettlementTransactionResponse> getSettlementTransactionsByStatus(String status);

    List<SettlementTransactionResponse> getSettlementTransactionsByType(String type);

    Page<SettlementTransactionResponse> getSettlementTransactions(
            Long paymentId,
            String accountNumber,
            String status,
            String transactionType,
            Pageable pageable);

    void processApproval(Transaction transaction, PACS008 pacs008);

    void processPacs002(Transaction transaction, PACS002 pacs002);

    void revertToSender(Long transactionId);
}
