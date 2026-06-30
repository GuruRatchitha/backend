package com.bank.fedwire.controller;

import com.bank.fedwire.dto.AccountStatisticsResponse;
import com.bank.fedwire.dto.DashboardApiResponse;
import com.bank.fedwire.dto.DashboardSettlementTransactionResponse;
import com.bank.fedwire.dto.DashboardSummaryResponse;
import com.bank.fedwire.dto.PendingBeneficiaryResponse;
import com.bank.fedwire.dto.PendingTransactionResponse;
import com.bank.fedwire.dto.RecentActivityResponse;
import com.bank.fedwire.dto.RecentCustomerResponse;
import com.bank.fedwire.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardApiResponse<DashboardSummaryResponse>> getSummary(
            @RequestHeader("X-User-Id") Long employeeUserId) {
        return ok("Dashboard summary retrieved successfully.", dashboardService.getSummary(employeeUserId));
    }

    @GetMapping("/recent-customers")
    public ResponseEntity<DashboardApiResponse<List<RecentCustomerResponse>>> getRecentCustomers(
            @RequestHeader("X-User-Id") Long employeeUserId) {
        return ok("Recent customers retrieved successfully.", dashboardService.getRecentCustomers(employeeUserId));
    }

    @GetMapping("/pending-beneficiaries")
    public ResponseEntity<DashboardApiResponse<List<PendingBeneficiaryResponse>>> getPendingBeneficiaries(
            @RequestHeader("X-User-Id") Long employeeUserId) {
        return ok("Pending beneficiaries retrieved successfully.", dashboardService.getPendingBeneficiaries(employeeUserId));
    }

    @GetMapping("/pending-transactions")
    public ResponseEntity<DashboardApiResponse<List<PendingTransactionResponse>>> getPendingTransactions(
            @RequestHeader("X-User-Id") Long employeeUserId) {
        return ok("Pending transactions retrieved successfully.", dashboardService.getPendingTransactions(employeeUserId));
    }

    @GetMapping("/account-statistics")
    public ResponseEntity<DashboardApiResponse<AccountStatisticsResponse>> getAccountStatistics(
            @RequestHeader("X-User-Id") Long employeeUserId) {
        return ok("Account statistics retrieved successfully.", dashboardService.getAccountStatistics(employeeUserId));
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<DashboardApiResponse<List<RecentActivityResponse>>> getRecentActivity(
            @RequestHeader("X-User-Id") Long employeeUserId) {
        return ok("Recent activity retrieved successfully.", dashboardService.getRecentActivities(employeeUserId));
    }

    @GetMapping("/recent-settlement-transactions")
    public ResponseEntity<DashboardApiResponse<List<DashboardSettlementTransactionResponse>>> getRecentSettlementTransactions(
            @RequestHeader("X-User-Id") Long employeeUserId) {
        return ok("Recent settlement transactions retrieved successfully.",
                dashboardService.getRecentSettlementTransactions(employeeUserId));
    }

    private <T> ResponseEntity<DashboardApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(DashboardApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build());
    }
}
