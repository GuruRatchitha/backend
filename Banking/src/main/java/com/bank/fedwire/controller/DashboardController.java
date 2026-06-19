package com.bank.fedwire.controller;

import com.bank.fedwire.dto.DashboardSummaryResponse;
import com.bank.fedwire.dto.TransactionPageResponse;
import com.bank.fedwire.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<DashboardSummaryResponse> getSummary(@RequestParam Long userId) {
        return ResponseEntity.ok(dashboardService.getSummary(userId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<TransactionPageResponse> getTransactions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "all") String limit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(dashboardService.getTransactions(userId, limit, page, size));
    }
}
