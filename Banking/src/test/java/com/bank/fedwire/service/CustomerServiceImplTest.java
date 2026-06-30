package com.bank.fedwire.service;

import com.bank.fedwire.dto.CustomerRequest;
import com.bank.fedwire.dto.CustomerResponse;
import com.bank.fedwire.dto.CustomerAccountRequest;
import com.bank.fedwire.dto.CustomerUpdateRequest;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.Role;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.DashboardActivityRepository;
import com.bank.fedwire.repository.RoleRepository;
import com.bank.fedwire.repository.UserRepository;
import com.bank.fedwire.util.AccountNumberGenerator;
import com.bank.fedwire.util.IbanGenerator;
import com.bank.fedwire.util.RoutingNumberGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceImplTest {

    @Test
    void createCustomerSavesUserAndLinkedAccountInOneTransaction() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        DashboardActivityRepository dashboardActivityRepository = mock(DashboardActivityRepository.class);
        AccountNumberGenerator accountNumberGenerator = mock(AccountNumberGenerator.class);
        RoutingNumberGenerator routingNumberGenerator = mock(RoutingNumberGenerator.class);
        IbanGenerator ibanGenerator = new IbanGenerator();

        when(roleRepository.findById(2L))
                .thenReturn(Optional.of(Role.builder().roleId(2L).roleName("CUSTOMER").build()));
        when(accountNumberGenerator.generate()).thenReturn("21234567890");
        when(routingNumberGenerator.generate()).thenReturn("123456789");
        when(accountRepository.existsByAccountNumber("21234567890")).thenReturn(false);
        when(accountRepository.existsByRoutingNumber("123456789")).thenReturn(false);
        when(accountRepository.existsByIban("US2123456789000000077")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(42L);
            return user;
        });
        when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setAccountId(77L);
            return account;
        });
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerServiceImpl service = new CustomerServiceImpl(
                userRepository,
                roleRepository,
                accountRepository,
                accountNumberGenerator,
                ibanGenerator,
                routingNumberGenerator,
                dashboardActivityRepository);

        CustomerResponse response = service.createCustomer(CustomerRequest.builder()
                .fullName("Ravi Kumar")
                .email("ravi.kumar@example.com")
                .password("Password@123")
                .phoneNumber("9876543210")
                .aadharNumber("123456789012")
                .panCardNumber("ABCDE1234F")
                .address("123 Main Street")
                .townName("Chennai")
                .accountType("savings")
                .initialBalance(new BigDecimal("500.00"))
                .build());

        Method createCustomer = CustomerServiceImpl.class.getMethod("createCustomer", CustomerRequest.class);
        assertThat(createCustomer.getAnnotation(Transactional.class)).isNotNull();
        assertThat(response.getUserId()).isEqualTo(42L);
        assertThat(response.getAccounts()).hasSize(1);
        assertThat(response.getAccounts().get(0).getUserId()).isEqualTo(42L);
        assertThat(response.getAccounts().get(0).getBalance()).isEqualByComparingTo("500.00");
        assertThat(response.getAccounts().get(0).getAccountType()).isEqualTo("SAVINGS");
        assertThat(response.getAccounts().get(0).getAccountNumber()).isEqualTo("21234567890");
        assertThat(response.getAccounts().get(0).getIban()).isEqualTo("US2123456789000000077");
        assertThat(response.getAccounts().get(0).getRoutingNumber()).isEqualTo("123456789");
        assertThat(response.getAccounts().get(0).getCurrency()).isEqualTo("USD");
        assertThat(response.getAccounts().get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getAccounts().get(0).getCreatedDate()).isNotNull();
    }

    @Test
    void createCustomerSavesEveryRequestedAccount() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        DashboardActivityRepository dashboardActivityRepository = mock(DashboardActivityRepository.class);
        AccountNumberGenerator accountNumberGenerator = mock(AccountNumberGenerator.class);
        RoutingNumberGenerator routingNumberGenerator = mock(RoutingNumberGenerator.class);
        IbanGenerator ibanGenerator = new IbanGenerator();

        when(roleRepository.findById(2L))
                .thenReturn(Optional.of(Role.builder().roleId(2L).roleName("CUSTOMER").build()));
        when(accountNumberGenerator.generate()).thenReturn("21234567890", "29876543210", "27654321098");
        when(routingNumberGenerator.generate()).thenReturn("123456789", "234567891", "345678912");
        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.existsByRoutingNumber(any())).thenReturn(false);
        when(accountRepository.existsByIban(any())).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(42L);
            return user;
        });
        long[] accountId = {76L};
        when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setAccountId(++accountId[0]);
            return account;
        });
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerServiceImpl service = new CustomerServiceImpl(
                userRepository,
                roleRepository,
                accountRepository,
                accountNumberGenerator,
                ibanGenerator,
                routingNumberGenerator,
                dashboardActivityRepository);

        CustomerResponse response = service.createCustomer(CustomerRequest.builder()
                .fullName("Ravi Kumar")
                .email("ravi.kumar@example.com")
                .password("Password@123")
                .phoneNumber("9876543210")
                .aadharNumber("123456789012")
                .panCardNumber("ABCDE1234F")
                .address("123 Main Street")
                .townName("Chennai")
                .accounts(List.of(
                        CustomerAccountRequest.builder()
                                .accountType("savings")
                                .balance(new BigDecimal("500.00"))
                                .build(),
                        CustomerAccountRequest.builder()
                                .accountType("current")
                                .balance(new BigDecimal("1500.00"))
                                .build(),
                        CustomerAccountRequest.builder()
                                .accountType("salary")
                                .balance(new BigDecimal("2500.00"))
                                .build()))
                .build());

        assertThat(response.getAccounts()).hasSize(3);
        assertThat(response.getAccounts())
                .extracting("accountType")
                .containsExactly("SAVINGS", "CURRENT", "SALARY");
        assertThat(response.getAccounts())
                .extracting("balance")
                .containsExactly(new BigDecimal("500.00"), new BigDecimal("1500.00"), new BigDecimal("2500.00"));
        assertThat(response.getAccounts())
                .extracting("accountNumber")
                .containsExactly("21234567890", "29876543210", "27654321098");
        assertThat(response.getAccounts())
                .extracting("routingNumber")
                .containsExactly("123456789", "234567891", "345678912");
        assertThat(response.getAccounts()).allSatisfy(account -> {
            assertThat(account.getAccountNumber()).startsWith("2").hasSize(11);
            assertThat(account.getIban()).startsWith("US");
            assertThat(account.getCurrency()).isEqualTo("USD");
            assertThat(account.getStatus()).isEqualTo("ACTIVE");
            assertThat(account.getUserId()).isEqualTo(42L);
        });

        verify(accountRepository, times(3)).saveAndFlush(any(Account.class));
        verify(accountRepository, times(3)).save(any(Account.class));
    }

    @Test
    void updateCustomerUpdatesUserExistingAccountAndCreatesNewAccountInOneTransaction() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        DashboardActivityRepository dashboardActivityRepository = mock(DashboardActivityRepository.class);
        AccountNumberGenerator accountNumberGenerator = mock(AccountNumberGenerator.class);
        RoutingNumberGenerator routingNumberGenerator = mock(RoutingNumberGenerator.class);
        IbanGenerator ibanGenerator = new IbanGenerator();

        User user = User.builder()
                .userId(7L)
                .userName("Jackson")
                .email("old.jackson@gmail.com")
                .password("old")
                .phoneNumber("8765467812")
                .aadharNumber("589678963256")
                .panCardNumber("HYTRF4567F")
                .address("Old Address")
                .townName("Chennai")
                .countryCode("US")
                .createdDate(LocalDateTime.now())
                .role(Role.builder().roleId(2L).roleName("CUSTOMER").build())
                .accounts(new ArrayList<>())
                .build();
        Account existingAccount = Account.builder()
                .accountId(12L)
                .user(user)
                .accountNumber("23456789012")
                .iban("US23456789012000000012")
                .routingNumber("456789123")
                .balance(new BigDecimal("5000.00"))
                .accountType("CURRENT")
                .currency("USD")
                .status("ACTIVE")
                .createdDate(LocalDateTime.now().minusDays(1))
                .build();
        user.getAccounts().add(existingAccount);

        when(userRepository.findWithRoleAndAccountsByUserId(7L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndUserIdNot("jackson.new@gmail.com", 7L)).thenReturn(false);
        when(userRepository.existsByAadharNumberAndUserIdNot("589678963256", 7L)).thenReturn(false);
        when(userRepository.existsByPanCardNumberIgnoreCaseAndUserIdNot("HYTRF4567F", 7L)).thenReturn(false);
        when(accountNumberGenerator.generate()).thenReturn("29876543210");
        when(routingNumberGenerator.generate()).thenReturn("567891234");
        when(accountRepository.existsByAccountNumber("29876543210")).thenReturn(false);
        when(accountRepository.existsByRoutingNumber("567891234")).thenReturn(false);
        when(accountRepository.existsByIban("US2987654321000000088")).thenReturn(false);
        when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setAccountId(88L);
            return account;
        });
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerServiceImpl service = new CustomerServiceImpl(
                userRepository,
                roleRepository,
                accountRepository,
                accountNumberGenerator,
                ibanGenerator,
                routingNumberGenerator,
                dashboardActivityRepository);

        CustomerResponse response = service.updateCustomer(7L, CustomerUpdateRequest.builder()
                .userName("Jackson New")
                .email("jackson.new@gmail.com")
                .password("new-password")
                .phoneNumber("8765467812")
                .aadhaarNumber("589678963256")
                .panCardNumber("HYTRF4567F")
                .address("23 West Avenue")
                .townName("Chennai")
                .countryCode("us")
                .accounts(List.of(
                        CustomerAccountRequest.builder()
                                .accountId(12L)
                                .balance(new BigDecimal("7500.00"))
                                .accountType("salary")
                                .status("inactive")
                                .build(),
                        CustomerAccountRequest.builder()
                                .balance(new BigDecimal("1000.00"))
                                .accountType("current")
                                .build()))
                .build());

        Method updateCustomer = CustomerServiceImpl.class.getMethod("updateCustomer", Long.class, CustomerUpdateRequest.class);
        assertThat(updateCustomer.getAnnotation(Transactional.class)).isNotNull();
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getUserName()).isEqualTo("Jackson New");
        assertThat(response.getEmail()).isEqualTo("jackson.new@gmail.com");
        assertThat(response.getAddress()).isEqualTo("23 West Avenue");
        assertThat(response.getCountryCode()).isEqualTo("US");
        assertThat(response.getAccounts()).hasSize(2);

        Account updatedExistingAccount = user.getAccounts().stream()
                .filter(account -> account.getAccountId().equals(12L))
                .findFirst()
                .orElseThrow();
        assertThat(updatedExistingAccount.getBalance()).isEqualByComparingTo("7500.00");
        assertThat(updatedExistingAccount.getAccountType()).isEqualTo("SALARY");
        assertThat(updatedExistingAccount.getStatus()).isEqualTo("INACTIVE");
        assertThat(updatedExistingAccount.getAccountNumber()).isEqualTo("23456789012");
        assertThat(updatedExistingAccount.getIban()).isEqualTo("US23456789012000000012");
        assertThat(updatedExistingAccount.getRoutingNumber()).isEqualTo("456789123");

        Account newAccount = user.getAccounts().stream()
                .filter(account -> account.getAccountId().equals(88L))
                .findFirst()
                .orElseThrow();
        assertThat(newAccount.getUser().getUserId()).isEqualTo(7L);
        assertThat(newAccount.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(newAccount.getAccountType()).isEqualTo("CURRENT");
        assertThat(newAccount.getAccountNumber()).isEqualTo("29876543210");
        assertThat(newAccount.getIban()).isEqualTo("US2987654321000000088");
        assertThat(newAccount.getRoutingNumber()).isEqualTo("567891234");
        assertThat(newAccount.getCurrency()).isEqualTo("USD");
        assertThat(newAccount.getStatus()).isEqualTo("ACTIVE");
        assertThat(newAccount.getCreatedDate()).isNotNull();

        verify(userRepository).save(user);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }
}
