package com.bank.fedwire.service;

import com.bank.fedwire.dto.AccountStatisticsResponse;
import com.bank.fedwire.dto.DashboardSummaryResponse;
import com.bank.fedwire.dto.PendingBeneficiaryResponse;
import com.bank.fedwire.dto.PendingTransactionResponse;
import com.bank.fedwire.dto.RecentActivityResponse;
import com.bank.fedwire.dto.RecentCustomerResponse;
import com.bank.fedwire.entity.Role;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.DashboardActivityRepository;
import com.bank.fedwire.repository.TransactionRepository;
import com.bank.fedwire.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DashboardActivityRepository dashboardActivityRepository;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                userRepository,
                accountRepository,
                beneficiaryRepository,
                transactionRepository,
                dashboardActivityRepository);

        when(userRepository.findWithRoleByUserId(1L)).thenReturn(Optional.of(User.builder()
                .userId(1L)
                .role(Role.builder().roleName("EMPLOYEE").build())
                .build()));
    }

    @Test
    void summaryUsesAggregateRepositoryQueries() {
        when(userRepository.countByRoleRoleNameIgnoreCase("CUSTOMER")).thenReturn(125L);
        when(accountRepository.count()).thenReturn(210L);
        when(accountRepository.sumTotalBalance()).thenReturn(new BigDecimal("4850000.75"));
        when(beneficiaryRepository.countByStatusIgnoreCase("PENDING")).thenReturn(8L);
        when(transactionRepository.countByTransactionStatusIgnoreCase("PENDING")).thenReturn(14L);
        when(userRepository.countByRoleRoleNameIgnoreCaseAndCreatedDateBetween(eq("CUSTOMER"), any(), any()))
                .thenReturn(6L);

        DashboardSummaryResponse response = dashboardService.getSummary(1L);

        assertThat(response.getTotalCustomers()).isEqualTo(125L);
        assertThat(response.getTotalAccounts()).isEqualTo(210L);
        assertThat(response.getTotalBankBalance()).isEqualByComparingTo("4850000.75");
        assertThat(response.getPendingBeneficiaries()).isEqualTo(8L);
        assertThat(response.getPendingTransactions()).isEqualTo(14L);
        assertThat(response.getTodayCustomers()).isEqualTo(6L);
    }

    @Test
    void accountStatisticsMapsGroupedAccountTypeCounts() {
        when(accountRepository.countAccountsByType()).thenReturn(List.of(
                new Object[]{"SAVINGS", 42L},
                new Object[]{"CURRENT", 31L},
                new Object[]{"SALARY", 18L}
        ));

        AccountStatisticsResponse response = dashboardService.getAccountStatistics(1L);

        assertThat(response.getSavings()).isEqualTo(42L);
        assertThat(response.getCurrent()).isEqualTo(31L);
        assertThat(response.getSalary()).isEqualTo(18L);
    }

    @Test
    void dashboardListsReturnProjectionData() {
        LocalDateTime now = LocalDateTime.now();
        RecentCustomerResponse customer = RecentCustomerResponse.builder()
                .userId(7L)
                .userName("Jackson")
                .createdDate(now)
                .build();
        PendingBeneficiaryResponse beneficiary = PendingBeneficiaryResponse.builder()
                .beneficiaryId(12L)
                .status("PENDING")
                .requestedDate(now)
                .build();
        PendingTransactionResponse transaction = PendingTransactionResponse.builder()
                .transactionId(22L)
                .status("PENDING")
                .createdDate(now)
                .build();
        RecentActivityResponse activity = RecentActivityResponse.builder()
                .activity("Customer Created")
                .timestamp(now)
                .build();

        when(userRepository.findRecentCustomers(eq("CUSTOMER"), any())).thenReturn(List.of(customer));
        when(beneficiaryRepository.findPendingBeneficiaries(eq("PENDING"), any())).thenReturn(List.of(beneficiary));
        when(transactionRepository.findPendingTransactions(eq("PENDING"), any())).thenReturn(List.of(transaction));
        when(dashboardActivityRepository.findRecentActivities(any())).thenReturn(List.of(activity));

        assertThat(dashboardService.getRecentCustomers(1L)).containsExactly(customer);
        assertThat(dashboardService.getPendingBeneficiaries(1L)).containsExactly(beneficiary);
        assertThat(dashboardService.getPendingTransactions(1L)).containsExactly(transaction);
        assertThat(dashboardService.getRecentActivities(1L)).containsExactly(activity);
    }
}
