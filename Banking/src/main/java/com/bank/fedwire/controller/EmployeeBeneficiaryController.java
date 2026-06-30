package com.bank.fedwire.controller;

import com.bank.fedwire.dto.BeneficiaryCreateResponse;
import com.bank.fedwire.dto.BeneficiaryResponse;
import com.bank.fedwire.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
        return ResponseEntity.ok(beneficiaryService.getBeneficiariesByStatus("pending"));
    }

    @GetMapping("/approved")
    public ResponseEntity<List<BeneficiaryResponse>> getApprovedBeneficiaries() {
        return ResponseEntity.ok(beneficiaryService.getBeneficiariesByStatus("approved"));
    }

    @GetMapping("/rejected")
    public ResponseEntity<List<BeneficiaryResponse>> getRejectedBeneficiaries() {
        return ResponseEntity.ok(beneficiaryService.getBeneficiariesByStatus("rejected"));
    }

    @GetMapping("/{status}")
    public ResponseEntity<List<BeneficiaryResponse>> getBeneficiariesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiariesByStatus(status));
    }

    @PutMapping("/{beneficiaryId}/approve")
    public ResponseEntity<BeneficiaryResponse> approveBeneficiary(@PathVariable Long beneficiaryId) {
        return ResponseEntity.ok(beneficiaryService.approveBeneficiary(beneficiaryId));
    }

    @PutMapping("/{userId}/{accountNumber}/{routingNumber}/approve")
    public ResponseEntity<BeneficiaryCreateResponse> approveBeneficiary(
            @PathVariable Long userId,
            @PathVariable String accountNumber,
            @PathVariable String routingNumber) {
        return ResponseEntity.ok(beneficiaryService.approveBeneficiary(userId, accountNumber, routingNumber));
    }

    @PutMapping("/{beneficiaryId}/reject")
    public ResponseEntity<BeneficiaryResponse> rejectBeneficiary(
            @PathVariable Long beneficiaryId,
            @RequestBody(required = false) Map<String, String> request) {
        String rejectionReason = request != null ? request.get("rejectionReason") : null;
        return ResponseEntity.ok(beneficiaryService.rejectBeneficiary(beneficiaryId, rejectionReason));
    }

    @PutMapping("/{userId}/{accountNumber}/{routingNumber}/reject")
    public ResponseEntity<BeneficiaryCreateResponse> rejectBeneficiary(
            @PathVariable Long userId,
            @PathVariable String accountNumber,
            @PathVariable String routingNumber,
            @RequestBody(required = false) Map<String, String> request) {
        String rejectionReason = request != null ? request.get("rejectionReason") : null;
        return ResponseEntity.ok(beneficiaryService.rejectBeneficiary(userId, accountNumber, routingNumber, rejectionReason));
    }
}
