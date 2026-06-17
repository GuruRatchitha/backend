package com.bank.fedwire.service;

import com.bank.fedwire.dto.DashboardSummaryResponse;

public interface DashboardService {

    DashboardSummaryResponse getSummary(Long userId);
}
