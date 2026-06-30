package com.bank.fedwire.service;

import com.bank.fedwire.dto.AccountRequest;
import com.bank.fedwire.dto.AccountResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.UserRepository;
import com.bank.fedwire.util.AccountNumberGenerator;
import com.bank.fedwire.util.IbanGenerator;
import com.bank.fedwire.util.RoutingNumberGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountServiceImplTest {

    @Test
    void createAccountUsesProvidedRoutingNumber() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AccountNumberGenerator accountNumberGenerator = mock(AccountNumberGenerator.class);
        RoutingNumberGenerator routingNumberGenerator = mock(RoutingNumberGenerator.class);

        when(userRepository.findById(7L)).thenReturn(Optional.of(User.builder().userId(7L).build()));
        when(accountNumberGenerator.generate()).thenReturn("21234567890");
        when(accountRepository.existsByAccountNumber("21234567890")).thenReturn(false);
        when(accountRepository.existsByRoutingNumber("123456789")).thenReturn(false);
        when(accountRepository.existsByIban("US2123456789000000077")).thenReturn(false);
        when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setAccountId(77L);
            return account;
        });
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountServiceImpl service = new AccountServiceImpl(
                accountRepository,
                userRepository,
                accountNumberGenerator,
                new IbanGenerator(),
                routingNumberGenerator);

        AccountResponse response = service.createAccount(AccountRequest.builder()
                .userId(7L)
                .accountType("savings")
                .balance(new BigDecimal("500.00"))
                .routingNumber("123456789")
                .build());

        assertThat(response.getAccountNumber()).isEqualTo("21234567890");
        assertThat(response.getIban()).isEqualTo("US2123456789000000077");
        assertThat(response.getRoutingNumber()).isEqualTo("123456789");
        assertThat(response.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void createAccountGeneratesUniqueRoutingNumberWhenMissing() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AccountNumberGenerator accountNumberGenerator = mock(AccountNumberGenerator.class);
        RoutingNumberGenerator routingNumberGenerator = mock(RoutingNumberGenerator.class);

        when(userRepository.findById(7L)).thenReturn(Optional.of(User.builder().userId(7L).build()));
        when(accountNumberGenerator.generate()).thenReturn("21234567890");
        when(routingNumberGenerator.generate()).thenReturn("234567891");
        when(accountRepository.existsByAccountNumber("21234567890")).thenReturn(false);
        when(accountRepository.existsByRoutingNumber("234567891")).thenReturn(false);
        when(accountRepository.existsByIban("US2123456789000000077")).thenReturn(false);
        when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setAccountId(77L);
            return account;
        });
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountServiceImpl service = new AccountServiceImpl(
                accountRepository,
                userRepository,
                accountNumberGenerator,
                new IbanGenerator(),
                routingNumberGenerator);

        AccountResponse response = service.createAccount(AccountRequest.builder()
                .userId(7L)
                .accountType("savings")
                .balance(new BigDecimal("500.00"))
                .build());

        assertThat(response.getRoutingNumber()).isEqualTo("234567891");
    }

    @Test
    void createAccountRejectsInvalidRoutingNumber() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AccountNumberGenerator accountNumberGenerator = mock(AccountNumberGenerator.class);
        RoutingNumberGenerator routingNumberGenerator = mock(RoutingNumberGenerator.class);

        when(userRepository.findById(7L)).thenReturn(Optional.of(User.builder().userId(7L).build()));
        when(accountNumberGenerator.generate()).thenReturn("21234567890");
        when(accountRepository.existsByAccountNumber("21234567890")).thenReturn(false);

        AccountServiceImpl service = new AccountServiceImpl(
                accountRepository,
                userRepository,
                accountNumberGenerator,
                new IbanGenerator(),
                routingNumberGenerator);

        assertThatThrownBy(() -> service.createAccount(AccountRequest.builder()
                .userId(7L)
                .accountType("savings")
                .balance(new BigDecimal("500.00"))
                .routingNumber("12345A789")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Routing number must be exactly 9 digits.");
    }
}
