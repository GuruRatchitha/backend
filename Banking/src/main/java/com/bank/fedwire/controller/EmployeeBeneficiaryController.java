package com.bank.fedwire.controller;

import com.bank.fedwire.dto.BeneficiaryResponse;
import com.bank.fedwire.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @RequestMapping(path = "/{beneficiaryId}/approve", method = {
            RequestMethod.PUT,
            RequestMethod.POST,
            RequestMethod.PATCH
    })
    public ResponseEntity<BeneficiaryResponse> approveBeneficiary(@PathVariable Long beneficiaryId) {
        return ResponseEntity.ok(beneficiaryService.approveBeneficiary(beneficiaryId));
    }

    @RequestMapping(path = "/{beneficiaryId}/reject", method = {
            RequestMethod.PUT,
            RequestMethod.POST,
            RequestMethod.PATCH
    })
    public ResponseEntity<BeneficiaryResponse> rejectBeneficiary(
            @PathVariable Long beneficiaryId,
            @RequestBody(required = false) Object request) {
        String rejectionReason = extractRejectionReason(request);
        return ResponseEntity.ok(beneficiaryService.rejectBeneficiary(beneficiaryId, rejectionReason));
    }

    private String extractRejectionReason(Object request) {
        if (request instanceof Map<?, ?> values) {
            Object value = values.get("rejectionReason");
            if (value == null) {
                value = values.get("reason");
            }
            return value != null ? value.toString() : null;
        }
        if (request instanceof String value) {
            return value;
        }
        return null;
    }
}
