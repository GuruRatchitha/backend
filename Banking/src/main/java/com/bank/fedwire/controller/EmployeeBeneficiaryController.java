package com.bank.fedwire.controller;

import com.bank.fedwire.dto.BeneficiaryCreateResponse;
import com.bank.fedwire.dto.BeneficiaryResponse;
import com.bank.fedwire.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/employee/beneficiaries")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
@RequiredArgsConstructor
public class EmployeeBeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @GetMapping("/pending")
    public ResponseEntity<List<BeneficiaryResponse>> getPendingBeneficiaries() {
        return ResponseEntity.ok(beneficiaryService.getPendingBeneficiaries());
    }

    @PutMapping("/{userId}/{accountNumber}/{routingNumber}/approve")
    public ResponseEntity<BeneficiaryCreateResponse> approveBeneficiary(
            @PathVariable Long userId,
            @PathVariable String accountNumber,
            @PathVariable String routingNumber) {
        validateUserId(userId);

        // Approves the existing pending beneficiary record instead of creating a new row.
        return ResponseEntity.ok(beneficiaryService.approveBeneficiary(userId, accountNumber, routingNumber));
    }

    @PutMapping("/{userId}/{accountNumber}/{routingNumber}/reject")
    public ResponseEntity<BeneficiaryCreateResponse> rejectBeneficiary(
            @PathVariable Long userId,
            @PathVariable String accountNumber,
            @PathVariable String routingNumber) {
        validateUserId(userId);

        // Rejects the existing pending beneficiary record so the customer can still see its final status.
        return ResponseEntity.ok(beneficiaryService.rejectBeneficiary(userId, accountNumber, routingNumber));
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
    }
}
