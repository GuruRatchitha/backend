package com.bank.fedwire.controller;

import com.bank.fedwire.dto.TransactionDetailResponse;
import com.bank.fedwire.dto.EmployeeTransactionQueueResponse;
import com.bank.fedwire.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employee/transactions")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
@RequiredArgsConstructor
public class EmployeeTransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<EmployeeTransactionQueueResponse>> getQueue() {
        return ResponseEntity.ok(transactionService.getEmployeeTransactionQueue());
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDetailResponse> getTransactionDetails(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getEmployeeTransactionDetails(transactionId));
    }

    @GetMapping(value = "/{transactionId}/pacs008", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getPacs008Xml(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.getEmployeeTransactionPacs008Xml(transactionId));
    }

    @PutMapping("/{transactionId}/hold")
    public ResponseEntity<TransactionDetailResponse> holdTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.holdTransaction(transactionId));
    }

    @PutMapping("/{transactionId}/reject")
    public ResponseEntity<TransactionDetailResponse> rejectTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.rejectTransaction(transactionId));
    }

    @PutMapping(value = "/{transactionId}/approve", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> approveTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(transactionService.approveTransaction(transactionId));
    }
}
