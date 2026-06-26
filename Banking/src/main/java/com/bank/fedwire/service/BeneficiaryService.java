package com.bank.fedwire.service;

import com.bank.fedwire.dto.BeneficiaryCreateResponse;
import com.bank.fedwire.dto.BeneficiaryRequest;
import com.bank.fedwire.dto.BeneficiaryResponse;

import java.util.List;

public interface BeneficiaryService {

    BeneficiaryCreateResponse addBeneficiary(Long userId, BeneficiaryRequest request);

    List<BeneficiaryResponse> getBeneficiaries(Long userId);

    List<BeneficiaryResponse> getPendingBeneficiaries();

    List<BeneficiaryResponse> getApprovedBeneficiaries();

    List<BeneficiaryResponse> getRejectedBeneficiaries();

    List<BeneficiaryResponse> getBeneficiariesByStatus(String status);

    BeneficiaryCreateResponse approveBeneficiary(Long userId, String accountNumber, String routingNumber);

    BeneficiaryCreateResponse rejectBeneficiary(Long userId, String accountNumber, String routingNumber, String rejectionReason);
}
