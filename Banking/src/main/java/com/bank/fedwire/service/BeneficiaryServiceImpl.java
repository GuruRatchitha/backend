package com.bank.fedwire.service;

import com.bank.fedwire.dto.BeneficiaryCreateResponse;
import com.bank.fedwire.dto.BeneficiaryRequest;
import com.bank.fedwire.dto.BeneficiaryResponse;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.BeneficiaryId;
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
                // New customer beneficiaries must wait for employee approval before payment is enabled.
                .status("PENDING")
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

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getPendingBeneficiaries() {
        return beneficiaryRepository.findByStatusOrderByCreatedDateDesc("PENDING").stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BeneficiaryCreateResponse approveBeneficiary(Long userId, String accountNumber, String routingNumber) {
        Beneficiary beneficiary = getBeneficiary(userId, accountNumber, routingNumber);
        beneficiary.setStatus("ACTIVE");
        beneficiary.setRejectionReason(null);

        return BeneficiaryCreateResponse.builder()
                .message("Beneficiary approved successfully")
                .beneficiary(toResponse(beneficiaryRepository.save(beneficiary)))
                .build();
    }

    @Override
    @Transactional
    public BeneficiaryCreateResponse rejectBeneficiary(Long userId, String accountNumber, String routingNumber, String rejectionReason) {
        requireText(rejectionReason, "rejectionReason is required");

        Beneficiary beneficiary = getBeneficiary(userId, accountNumber, routingNumber);
        beneficiary.setStatus("REJECTED");
        // Save the employee's rejection reason on the same beneficiary row for the customer list API.
        beneficiary.setRejectionReason(rejectionReason.trim());

        return BeneficiaryCreateResponse.builder()
                .message("Beneficiary rejected successfully")
                .beneficiary(toResponse(beneficiaryRepository.save(beneficiary)))
                .build();
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
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private Beneficiary getBeneficiary(Long userId, String accountNumber, String routingNumber) {
        requireText(accountNumber, "accountNumber is required");
        requireText(routingNumber, "routingNumber is required");

        // Beneficiary rows use a composite key, so the employee action identifies the same saved record by all key parts.
        BeneficiaryId beneficiaryId = new BeneficiaryId(userId, accountNumber.trim(), routingNumber.trim());
        return beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
    }

    private BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        String customerName = beneficiary.getUser() != null ? beneficiary.getUser().getUserName() : null;

        return BeneficiaryResponse.builder()
                .userId(beneficiary.getUserId())
                .customerName(customerName)
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .townName(beneficiary.getTownName())
                .countryCode(beneficiary.getCountryCode())
                .accountNumber(beneficiary.getAccountNumber())
                .routingNumber(beneficiary.getRoutingNumber())
                .createdDate(beneficiary.getCreatedDate())
                .status(beneficiary.getStatus())
                .rejectionReason(beneficiary.getRejectionReason())
                .build();
    }
}
