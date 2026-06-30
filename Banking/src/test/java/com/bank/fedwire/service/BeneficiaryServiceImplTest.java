package com.bank.fedwire.service;

import com.bank.fedwire.dto.BeneficiaryResponse;
import com.bank.fedwire.entity.Beneficiary;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.DashboardActivityRepository;
import com.bank.fedwire.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeneficiaryServiceImplTest {

    private final BeneficiaryRepository beneficiaryRepository = mock(BeneficiaryRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DashboardActivityRepository dashboardActivityRepository = mock(DashboardActivityRepository.class);
    private final BeneficiaryServiceImpl service = new BeneficiaryServiceImpl(
            beneficiaryRepository,
            userRepository,
            dashboardActivityRepository);

    @Test
    void approveBeneficiaryUpdatesPendingBeneficiaryAndReturnsResponse() {
        Beneficiary beneficiary = pendingBeneficiary();
        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(beneficiary));
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BeneficiaryResponse response = service.approveBeneficiary(10L);

        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getBeneficiaryId()).isEqualTo(10L);
        assertThat(response.getRejectionReason()).isNull();
        verify(beneficiaryRepository).save(beneficiary);
    }

    @Test
    void rejectBeneficiaryUpdatesPendingBeneficiaryAndReturnsResponse() {
        Beneficiary beneficiary = pendingBeneficiary();
        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(beneficiary));
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BeneficiaryResponse response = service.rejectBeneficiary(10L, "Invalid account");

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getRejectionReason()).isEqualTo("Invalid account");
        verify(beneficiaryRepository).save(beneficiary);
    }

    @Test
    void approvalStillSucceedsWhenActivityLoggingFails() {
        Beneficiary beneficiary = pendingBeneficiary();
        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(beneficiary));
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dashboardActivityRepository.save(any()))
                .thenThrow(new DataAccessResourceFailureException("dashboard_activity unavailable"));

        assertThatCode(() -> service.approveBeneficiary(10L)).doesNotThrowAnyException();
        assertThat(beneficiary.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void nonPendingBeneficiaryReturnsConflictInsteadOfGenericServerError() {
        Beneficiary beneficiary = pendingBeneficiary();
        beneficiary.setStatus("APPROVED");
        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(beneficiary));

        assertThatThrownBy(() -> service.rejectBeneficiary(10L, "Invalid account"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT")
                .hasMessageContaining("Only pending beneficiaries can be updated");
    }

    private Beneficiary pendingBeneficiary() {
        return Beneficiary.builder()
                .beneficiaryId(10L)
                .userId(101L)
                .beneficiaryName("Alice Ben")
                .townName("Phoenix")
                .countryCode("US")
                .accountNumber("AC-001")
                .routingNumber("653060219")
                .bankName("ABC Bank")
                .status("PENDING")
                .build();
    }
}
