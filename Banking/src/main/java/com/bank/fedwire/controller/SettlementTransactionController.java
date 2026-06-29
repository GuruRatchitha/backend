package com.bank.fedwire.controller;

import com.bank.fedwire.dto.SettlementAccountResponse;
import com.bank.fedwire.dto.SettlementTransactionResponse;
import com.bank.fedwire.service.SettlementTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settlement")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
@RequiredArgsConstructor
public class SettlementTransactionController {

    private final SettlementTransactionService settlementTransactionService;

    @GetMapping("/account")
    public ResponseEntity<SettlementAccountResponse> getSettlementAccount() {
        return ResponseEntity.ok(settlementTransactionService.getSettlementAccount());
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<SettlementTransactionResponse>> getAllSettlementTransactions() {
        return ResponseEntity.ok(settlementTransactionService.getAllSettlementTransactions());
    }

    @GetMapping("/transactions/{paymentId}")
    public ResponseEntity<List<SettlementTransactionResponse>> getSettlementTransactionsByPaymentId(
            @PathVariable Long paymentId) {
        return ResponseEntity.ok(settlementTransactionService.getSettlementTransactionsByPaymentId(paymentId));
    }

    @GetMapping("/transactions/status/{status}")
    public ResponseEntity<List<SettlementTransactionResponse>> getSettlementTransactionsByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(settlementTransactionService.getSettlementTransactionsByStatus(status));
    }

    @GetMapping("/transactions/type/{type}")
    public ResponseEntity<List<SettlementTransactionResponse>> getSettlementTransactionsByType(
            @PathVariable String type) {
        return ResponseEntity.ok(settlementTransactionService.getSettlementTransactionsByType(type));
    }
}
