package com.bank.fedwire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private long totalCustomers;

    private long totalAccounts;

    private BigDecimal totalBankBalance;

    private long pendingBeneficiaries;

    private long pendingTransactions;

    private long todayCustomers;
}
