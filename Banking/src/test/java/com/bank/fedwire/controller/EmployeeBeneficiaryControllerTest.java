package com.bank.fedwire.controller;

import com.bank.fedwire.dto.BeneficiaryCreateResponse;
import com.bank.fedwire.dto.BeneficiaryResponse;
import com.bank.fedwire.service.BeneficiaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeBeneficiaryControllerTest {

    @Mock
    private BeneficiaryService beneficiaryService;

    @InjectMocks
    private EmployeeBeneficiaryController controller;

    @Test
    void pendingBeneficiariesReturnsPendingStatusList() {
        when(beneficiaryService.getBeneficiariesByStatus("pending")).thenReturn(sampleBeneficiaries("PENDING"));

        List<BeneficiaryResponse> body = controller.getPendingBeneficiaries().getBody();

        assertEquals("PENDING", body.get(0).getStatus());
        assertEquals(101L, body.get(0).getUserId());
        assertEquals("Alice Buyer", body.get(0).getCustomerName());
        assertEquals("Alice Ben", body.get(0).getBeneficiaryName());
        assertEquals("Phoenix", body.get(0).getTownName());
        assertEquals("US", body.get(0).getCountryCode());
        assertEquals("AC-001", body.get(0).getAccountNumber());
        assertEquals("653060219", body.get(0).getRoutingNumber());
        assertEquals(null, body.get(0).getRejectionReason());

        verify(beneficiaryService).getBeneficiariesByStatus("pending");
    }

    @Test
    void approvedBeneficiariesReturnsApprovedStatuses() {
        when(beneficiaryService.getBeneficiariesByStatus("approved")).thenReturn(sampleBeneficiaries("APPROVED"));

        List<BeneficiaryResponse> body = controller.getApprovedBeneficiaries().getBody();

        assertEquals("APPROVED", body.get(0).getStatus());
        assertEquals(101L, body.get(0).getUserId());
        assertEquals("Alice Buyer", body.get(0).getCustomerName());
        assertEquals(null, body.get(0).getRejectionReason());

        verify(beneficiaryService).getBeneficiariesByStatus("approved");
    }

    @Test
    void rejectedBeneficiariesReturnsRejectedStatusList() {
        when(beneficiaryService.getBeneficiariesByStatus("rejected")).thenReturn(sampleBeneficiaries("REJECTED"));

        List<BeneficiaryResponse> body = controller.getRejectedBeneficiaries().getBody();

        assertEquals("REJECTED", body.get(0).getStatus());
        assertEquals("Invalid account", body.get(0).getRejectionReason());

        verify(beneficiaryService).getBeneficiariesByStatus("rejected");
    }

    @Test
    void approveBeneficiarySupportsLegacyUserAccountRoutingRoute() {
        BeneficiaryCreateResponse response = BeneficiaryCreateResponse.builder()
                .message("Beneficiary approved successfully")
                .beneficiary(sampleBeneficiaries("APPROVED").get(0))
                .build();
        when(beneficiaryService.approveBeneficiary(2L, "123455678999945354", "987654321")).thenReturn(response);

        BeneficiaryCreateResponse body = controller
                .approveBeneficiary(2L, "123455678999945354", "987654321")
                .getBody();

        assertEquals("Beneficiary approved successfully", body.getMessage());
        verify(beneficiaryService).approveBeneficiary(2L, "123455678999945354", "987654321");
    }

    @Test
    void rejectBeneficiarySupportsLegacyUserAccountRoutingRoute() {
        BeneficiaryCreateResponse response = BeneficiaryCreateResponse.builder()
                .message("Beneficiary rejected successfully")
                .beneficiary(sampleBeneficiaries("REJECTED").get(0))
                .build();
        when(beneficiaryService.rejectBeneficiary(2L, "123455678999945354", "987654321", "bad data"))
                .thenReturn(response);

        BeneficiaryCreateResponse body = controller
                .rejectBeneficiary(2L, "123455678999945354", "987654321", Map.of("rejectionReason", "bad data"))
                .getBody();

        assertEquals("Beneficiary rejected successfully", body.getMessage());
        verify(beneficiaryService).rejectBeneficiary(2L, "123455678999945354", "987654321", "bad data");
    }

    private List<BeneficiaryResponse> sampleBeneficiaries(String status) {
        return List.of(BeneficiaryResponse.builder()
                .beneficiaryId(1L)
                .userId(101L)
                .customerName("Alice Buyer")
                .beneficiaryName("Alice Ben")
                .townName("Phoenix")
                .countryCode("US")
                .accountNumber("AC-001")
                .routingNumber("653060219")
                .createdDate(LocalDateTime.of(2026, 6, 25, 10, 0))
                .status(status)
                .rejectionReason("REJECTED".equals(status) ? "Invalid account" : null)
                .build());
    }
}
