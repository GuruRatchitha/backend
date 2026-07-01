package com.bank.fedwire.service;

import com.bank.fedwire.dto.AccountStatisticsResponse;
import com.bank.fedwire.dto.DashboardSummaryResponse;
import com.bank.fedwire.dto.PendingBeneficiaryResponse;
import com.bank.fedwire.dto.PendingTransactionResponse;
import com.bank.fedwire.dto.RecentActivityResponse;
import com.bank.fedwire.dto.RecentCustomerResponse;
import com.bank.fedwire.dto.SettlementTransactionResponse;

import java.util.List;

public interface DashboardService {

    DashboardSummaryResponse getSummary(Long employeeUserId);

    List<RecentCustomerResponse> getRecentCustomers(Long employeeUserId);

    List<PendingBeneficiaryResponse> getPendingBeneficiaries(Long employeeUserId);

    List<PendingTransactionResponse> getPendingTransactions(Long employeeUserId);

    AccountStatisticsResponse getAccountStatistics(Long employeeUserId);

    List<RecentActivityResponse> getRecentActivities(Long employeeUserId);

    List<SettlementTransactionResponse> getRecentSettlementTransactions(Long employeeUserId);
}
