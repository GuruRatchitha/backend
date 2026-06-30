package com.bank.fedwire.service;

import com.bank.fedwire.dto.BeneficiaryCreateResponse;
import com.bank.fedwire.dto.BeneficiaryRequest;
import com.bank.fedwire.dto.BeneficiaryResponse;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.entity.DashboardActivity;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.DashboardActivityRepository;
import com.bank.fedwire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final DashboardActivityRepository dashboardActivityRepository;

    @Override
    @Transactional
    public BeneficiaryCreateResponse addBeneficiary(Long userId, BeneficiaryRequest request) {
        validateRequest(request);

        // TEMPORARY FOR PAYAPT ADM.002 TESTING
        String accountNumber = request.getAccountNumber();
        // END TEMPORARY
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
                // TEMPORARY FOR PAYAPT ADM.002 TESTING
                .routingNumber(request.getRoutingNumber())
                // END TEMPORARY
                .bankName(normalizeBankName(request.getBankName()))
                .status(STATUS_PENDING)
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
        return beneficiaryRepository.findByStatusOrderByCreatedDateDesc(STATUS_PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getApprovedBeneficiaries() {
        return beneficiaryRepository.findByStatusInOrderByCreatedDateDesc(List.of("ACTIVE", "APPROVED")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getRejectedBeneficiaries() {
        return beneficiaryRepository.findByStatusOrderByCreatedDateDesc("REJECTED").stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiariesByStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pending" -> getPendingBeneficiaries();
            case "approved" -> getApprovedBeneficiaries();
            case "rejected" -> getRejectedBeneficiaries();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "status must be pending, approved, or rejected");
        };
    }

    @Override
    @Transactional
    public BeneficiaryResponse approveBeneficiary(Long beneficiaryId) {
        Beneficiary beneficiary = getBeneficiaryForEmployeeAction(beneficiaryId);
        Beneficiary savedBeneficiary = approveBeneficiaryRecord(beneficiary);
        logActivity("Beneficiary Approved", "Beneficiary " + savedBeneficiary.getBeneficiaryName() + " was approved.");
        return toResponse(savedBeneficiary);
    }

    @Override
    @Transactional
    public BeneficiaryCreateResponse approveBeneficiary(Long userId, String accountNumber, String routingNumber) {
        Beneficiary savedBeneficiary = approveBeneficiaryRecord(getBeneficiary(userId, accountNumber, routingNumber));

        return BeneficiaryCreateResponse.builder()
                .message("Beneficiary approved successfully")
                .beneficiary(toResponse(savedBeneficiary))
                .build();
    }

    @Override
    @Transactional
    public BeneficiaryResponse rejectBeneficiary(Long beneficiaryId, String rejectionReason) {
        Beneficiary beneficiary = getBeneficiaryForEmployeeAction(beneficiaryId);
        Beneficiary savedBeneficiary = rejectBeneficiaryRecord(beneficiary, rejectionReason);
        logActivity("Beneficiary Rejected", "Beneficiary " + savedBeneficiary.getBeneficiaryName() + " was rejected.");
        return toResponse(savedBeneficiary);
    }

    @Override
    @Transactional
    public BeneficiaryCreateResponse rejectBeneficiary(Long userId, String accountNumber, String routingNumber, String rejectionReason) {
        requireText(rejectionReason, "rejectionReason is required");
        Beneficiary savedBeneficiary = rejectBeneficiaryRecord(
                getBeneficiary(userId, accountNumber, routingNumber),
                rejectionReason);

        return BeneficiaryCreateResponse.builder()
                .message("Beneficiary rejected successfully")
                .beneficiary(toResponse(savedBeneficiary))
                .build();
    }

    private Beneficiary approveBeneficiaryRecord(Beneficiary beneficiary) {
        beneficiary.setStatus(STATUS_APPROVED);
        beneficiary.setRejectionReason(null);
        return beneficiaryRepository.save(beneficiary);
    }

    private Beneficiary rejectBeneficiaryRecord(Beneficiary beneficiary, String rejectionReason) {
        beneficiary.setStatus(STATUS_REJECTED);
        beneficiary.setRejectionReason(normalizeRejectionReason(rejectionReason));
        return beneficiaryRepository.save(beneficiary);
    }

    private Beneficiary getBeneficiaryForEmployeeAction(Long beneficiaryId) {
        if (beneficiaryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "beneficiaryId is required");
        }

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));

        if (!STATUS_PENDING.equalsIgnoreCase(beneficiary.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending beneficiaries can be updated");
        }

        return beneficiary;
    }

    private Beneficiary getBeneficiary(Long userId, String accountNumber, String routingNumber) {
        requireText(accountNumber, "accountNumber is required");
        requireText(routingNumber, "routingNumber is required");

        return beneficiaryRepository.findByUserIdAndAccountNumberAndRoutingNumber(
                        userId, accountNumber.trim(), routingNumber.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
    }

    private String normalizeRejectionReason(String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            return null;
        }

        String trimmedReason = rejectionReason.trim();
        if (trimmedReason.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rejectionReason must be 255 characters or less");
        }

        return trimmedReason;
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

    private BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        String customerName = beneficiary.getUser() != null ? beneficiary.getUser().getUserName() : null;

        return BeneficiaryResponse.builder()
                .beneficiaryId(beneficiary.getBeneficiaryId())
                .userId(beneficiary.getUserId())
                .customerName(customerName)
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .townName(beneficiary.getTownName())
                .countryCode(beneficiary.getCountryCode())
                .accountNumber(beneficiary.getAccountNumber())
                .routingNumber(beneficiary.getRoutingNumber())
                .bankName(beneficiary.getBankName())
                .createdDate(beneficiary.getCreatedDate())
                .status(beneficiary.getStatus())
                .rejectionReason(beneficiary.getRejectionReason())
                .build();
    }

    private String normalizeBankName(String bankName) {
        if (bankName == null || bankName.isBlank()) {
            return "N/A";
        }
        return bankName.trim();
    }

    private void logActivity(String activity, String description) {
        dashboardActivityRepository.save(DashboardActivity.builder()
                .activity(activity)
                .description(description)
                .employeeName("System")
                .build());
    }
}
