package com.bank.fedwire.service;

import com.bank.fedwire.dto.BeneficiaryCreateResponse;
import com.bank.fedwire.dto.BeneficiaryRequest;
import com.bank.fedwire.dto.BeneficiaryResponse;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BeneficiaryCreateResponse addBeneficiary(Long userId, BeneficiaryRequest request) {
        validateRequest(request);

        String accountNumber = request.getAccountNumber().trim();
        if (beneficiaryRepository.existsByUserIdAndAccountNumber(userId, accountNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Beneficiary already exists for this account number");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Beneficiary beneficiary = Beneficiary.builder()
                .userId(user.getUserId())
                .user(user)
                .beneficiaryName(request.getBeneficiaryName().trim())
                .townName(request.getTownName().trim())
                .countryCode("US")
                .accountNumber(accountNumber)
                .routingNumber(request.getRoutingNumber().trim())
                .status(normalizeStatus(request.getStatus()))
                .build();

        Beneficiary savedBeneficiary = beneficiaryRepository.save(beneficiary);

        return BeneficiaryCreateResponse.builder()
                .message("Beneficiary created successfully")
                .beneficiary(toResponse(savedBeneficiary))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiaries(Long userId) {
        return beneficiaryRepository.findByUserIdOrderByCreatedDateDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateRequest(BeneficiaryRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        requireText(request.getBeneficiaryName(), "beneficiaryName is required");
        requireText(request.getTownName(), "townName is required");
        requireText(request.getAccountNumber(), "accountNumber is required");
//        requireText(request.getConfirmAccountNumber(), "confirmAccountNumber is required");
        requireText(request.getRoutingNumber(), "routingNumber is required");

//        if (!request.getAccountNumber().trim().equals(request.getConfirmAccountNumber().trim())) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account number and confirm account number must match");
//        }
        if (request.getBeneficiaryName().trim().length() > 140) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "beneficiaryName must be 140 characters or less");
        }
        if (request.getTownName().trim().length() > 35) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "townName must be 35 characters or less");
        }
        normalizeStatus(request.getStatus());
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "inactive";
        }

        String normalizedStatus = status.trim().toLowerCase();
        if (!"active".equals(normalizedStatus) && !"inactive".equals(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be active or inactive");
        }
        return normalizedStatus;
    }

    private BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        return BeneficiaryResponse.builder()
                .userId(beneficiary.getUserId())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .townName(beneficiary.getTownName())
                .countryCode(beneficiary.getCountryCode())
                .accountNumber(beneficiary.getAccountNumber())
                .routingNumber(beneficiary.getRoutingNumber())
                .createdDate(beneficiary.getCreatedDate())
                .status(beneficiary.getStatus())
                .build();
    }
}
