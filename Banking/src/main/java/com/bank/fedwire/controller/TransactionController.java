package com.bank.fedwire.controller;

import com.bank.fedwire.dto.TransactionResponse;
import com.bank.fedwire.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "X-User-Id", required = false) Long queryUserId,
            @RequestParam(required = false) Integer limit) {
        Long userId = headerUserId != null ? headerUserId : queryUserId;

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id is required");
        }

        return ResponseEntity.ok(transactionService.getTransactions(userId, limit));
    }
}
