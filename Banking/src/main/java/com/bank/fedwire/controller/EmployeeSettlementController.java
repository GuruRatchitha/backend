package com.bank.fedwire.controller;

import com.bank.fedwire.dto.SettlementAccountResponse;
import com.bank.fedwire.dto.SettlementTransactionResponse;
import com.bank.fedwire.service.SettlementTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/employee")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
@RequiredArgsConstructor
public class EmployeeSettlementController {

    private static final String DEFAULT_SORT = "createdAt,desc";

    private final SettlementTransactionService settlementTransactionService;

    @GetMapping("/settlement-account")
    public ResponseEntity<SettlementAccountResponse> getSettlementAccount() {
        return ResponseEntity.ok(settlementTransactionService.getSettlementAccount());
    }

    @GetMapping("/settlement-transactions")
    public ResponseEntity<List<SettlementTransactionResponse>> getSettlementTransactions(
            @RequestParam(required = false) Long paymentId,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {

        if (page < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(BAD_REQUEST, "size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<SettlementTransactionResponse> transactionPage = settlementTransactionService.getSettlementTransactions(
                paymentId,
                accountNumber,
                status,
                transactionType,
                pageable);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Page", String.valueOf(transactionPage.getNumber()));
        headers.add("X-Size", String.valueOf(transactionPage.getSize()));
        headers.add("X-Total-Elements", String.valueOf(transactionPage.getTotalElements()));
        headers.add("X-Total-Pages", String.valueOf(transactionPage.getTotalPages()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(transactionPage.getContent());
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",", 2);
        String property = parts[0].trim();
        if (property.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "sort property is required");
        }
        if ("dateTime".equalsIgnoreCase(property)) {
            property = "createdAt";
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1 && !parts[1].isBlank()) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid sort direction " + parts[1], ex);
            }
        }

        return Sort.by(direction, property);
    }
}
