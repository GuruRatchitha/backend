package com.bank.fedwire.service;

import com.bank.fedwire.dto.DashboardSummaryResponse;
import com.bank.fedwire.dto.TransactionPageResponse;

public interface DashboardService {

    DashboardSummaryResponse getSummary(Long userId);

    TransactionPageResponse getTransactions(Long userId, String limit, int page, int size);
}
