package com.bank.fedwire.service;

import com.bank.fedwire.dto.AccountStatisticsResponse;
import com.bank.fedwire.dto.DashboardSettlementTransactionResponse;
import com.bank.fedwire.dto.DashboardSummaryResponse;
import com.bank.fedwire.dto.PendingBeneficiaryResponse;
import com.bank.fedwire.dto.PendingTransactionResponse;
import com.bank.fedwire.dto.RecentActivityResponse;
import com.bank.fedwire.dto.RecentCustomerResponse;
import com.bank.fedwire.dto.SettlementTransactionResponse;
import com.bank.fedwire.entity.Role;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.BeneficiaryRepository;
import com.bank.fedwire.repository.DashboardActivityRepository;
import com.bank.fedwire.repository.TransactionRepository;
import com.bank.fedwire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String STATUS_PENDING = "PENDING";
    private static final int DASHBOARD_LIMIT = 5;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransactionRepository transactionRepository;
    private final DashboardActivityRepository dashboardActivityRepository;
    private final SettlementTransactionService settlementTransactionService;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long employeeUserId) {
        requireEmployee(employeeUserId);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();

        return DashboardSummaryResponse.builder()
                .totalCustomers(userRepository.countByRoleRoleNameIgnoreCase(ROLE_CUSTOMER))
                .totalAccounts(accountRepository.count())
                .totalBankBalance(nullToZero(accountRepository.sumTotalBalance()))
                .pendingBeneficiaries(beneficiaryRepository.countByStatusIgnoreCase(STATUS_PENDING))
                .pendingTransactions(transactionRepository.countByTransactionStatusIgnoreCase(STATUS_PENDING))
                .todayCustomers(userRepository.countByRoleRoleNameIgnoreCaseAndCreatedDateBetween(
                        ROLE_CUSTOMER,
                        startOfDay,
                        startOfTomorrow))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentCustomerResponse> getRecentCustomers(Long employeeUserId) {
        requireEmployee(employeeUserId);
        return userRepository.findRecentCustomers(ROLE_CUSTOMER, latestFive());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingBeneficiaryResponse> getPendingBeneficiaries(Long employeeUserId) {
        requireEmployee(employeeUserId);
        return beneficiaryRepository.findPendingBeneficiaries(STATUS_PENDING, latestFive());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingTransactionResponse> getPendingTransactions(Long employeeUserId) {
        requireEmployee(employeeUserId);
        return transactionRepository.findPendingTransactions(STATUS_PENDING, latestFive());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountStatisticsResponse getAccountStatistics(Long employeeUserId) {
        requireEmployee(employeeUserId);

        long savings = 0;
        long current = 0;
        long salary = 0;

        for (Object[] row : accountRepository.countAccountsByType()) {
            String accountType = row[0] != null ? row[0].toString() : "";
            long count = row[1] instanceof Number number ? number.longValue() : 0;
            if ("SAVINGS".equals(accountType)) {
                savings = count;
            } else if ("CURRENT".equals(accountType)) {
                current = count;
            } else if ("SALARY".equals(accountType) || "SALERY".equals(accountType)) {
                salary += count;
            }
        }

        return AccountStatisticsResponse.builder()
                .savings(savings)
                .current(current)
                .salary(salary)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentActivityResponse> getRecentActivities(Long employeeUserId) {
        requireEmployee(employeeUserId);
        return dashboardActivityRepository.findRecentActivities(latestFive());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardSettlementTransactionResponse> getRecentSettlementTransactions(Long employeeUserId) {
        requireEmployee(employeeUserId);
        return settlementTransactionService.getSettlementTransactions(
                        null,
                        null,
                        null,
                        null,
                        latestFiveSettlementTransactions())
                .getContent()
                .stream()
                .map(this::toDashboardSettlementTransactionResponse)
                .toList();
    }

    private void requireEmployee(Long employeeUserId) {
        if (employeeUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
        }

        User user = userRepository.findWithRoleByUserId(employeeUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found."));
        Role role = user.getRole();
        String roleName = role != null ? role.getRoleName() : null;
        if (!ROLE_EMPLOYEE.equalsIgnoreCase(roleName)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only employees can access dashboard APIs.");
        }
    }

    private PageRequest latestFive() {
        return PageRequest.of(0, DASHBOARD_LIMIT);
    }

    private PageRequest latestFiveSettlementTransactions() {
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("settlementTransactionId"));
        return PageRequest.of(0, DASHBOARD_LIMIT, sort);
    }

    private DashboardSettlementTransactionResponse toDashboardSettlementTransactionResponse(
            SettlementTransactionResponse transaction) {
        return new DashboardSettlementTransactionResponse(
                transaction.paymentId(),
                transaction.accountNumber(),
                transaction.amount(),
                transaction.status(),
                transaction.createdAt());
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
