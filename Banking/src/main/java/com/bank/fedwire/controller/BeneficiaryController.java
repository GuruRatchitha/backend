package com.bank.fedwire.controller;

import com.bank.fedwire.dto.BeneficiaryCreateResponse;
import com.bank.fedwire.dto.BeneficiaryRequest;
import com.bank.fedwire.dto.BeneficiaryResponse;
import com.bank.fedwire.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    public ResponseEntity<BeneficiaryCreateResponse> addBeneficiary(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody BeneficiaryRequest request) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id is required");
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(beneficiaryService.addBeneficiary(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getBeneficiaries(
            @RequestHeader("X-User-Id") Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id is required");
        }

        return ResponseEntity.ok(beneficiaryService.getBeneficiaries(userId));
    }
}
