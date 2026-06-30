package com.bank.fedwire.service;

import com.bank.fedwire.dto.AccountRequest;
import com.bank.fedwire.dto.AccountResponse;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.exception.DuplicateAccountNumberException;
import com.bank.fedwire.exception.DuplicateResourceException;
import com.bank.fedwire.exception.ResourceNotFoundException;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.UserRepository;
import com.bank.fedwire.util.AccountNumberGenerator;
import com.bank.fedwire.util.IbanGenerator;
import com.bank.fedwire.util.RoutingNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 20;
    private static final int MAX_ROUTING_NUMBER_ATTEMPTS = 20;
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_STATUS = "ACTIVE";

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final IbanGenerator ibanGenerator;
    private final RoutingNumberGenerator routingNumberGenerator;

    @Override
    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Account account = Account.builder()
                .user(user)
                .accountNumber(generateUniqueAccountNumber())
                .routingNumber(resolveRoutingNumber(request.getRoutingNumber()))
                .accountType(normalizeAccountType(request.getAccountType()))
                .balance(request.getBalance())
                .currency(DEFAULT_CURRENCY)
                .status(DEFAULT_STATUS)
                .createdDate(LocalDateTime.now())
                .build();

        Account savedAccount;
        try {
            savedAccount = accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException ex) {
            if (isAccountNumberConstraintViolation(ex)) {
                throw new DuplicateAccountNumberException("Generated account number already exists.");
            }
            throw ex;
        }

        String iban = ibanGenerator.generate(savedAccount.getAccountNumber(), savedAccount.getAccountId());
        if (accountRepository.existsByIban(iban)) {
            throw new DuplicateResourceException("Generated IBAN already exists.");
        }

        savedAccount.setIban(iban);
        return toAccountResponse(accountRepository.save(savedAccount));
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            String accountNumber = accountNumberGenerator.generate();
            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }
        throw new DuplicateAccountNumberException("Unable to generate a unique account number.");
    }

    private String resolveRoutingNumber(String routingNumber) {
        String normalized = trim(routingNumber);
        if (normalized != null) {
            validateRoutingNumber(normalized);
            if (accountRepository.existsByRoutingNumber(normalized)) {
                throw new DuplicateResourceException("Routing number already exists.");
            }
            return normalized;
        }
        return generateUniqueRoutingNumber();
    }

    private String generateUniqueRoutingNumber() {
        for (int attempt = 0; attempt < MAX_ROUTING_NUMBER_ATTEMPTS; attempt++) {
            String routingNumber = routingNumberGenerator.generate();
            if (!accountRepository.existsByRoutingNumber(routingNumber)) {
                return routingNumber;
            }
        }
        throw new DuplicateResourceException("Unable to generate a unique routing number.");
    }

    private void validateRoutingNumber(String routingNumber) {
        if (!routingNumber.matches("\\d{9}")) {
            throw new IllegalArgumentException("Routing number must be exactly 9 digits.");
        }
    }

    private boolean isAccountNumberConstraintViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return message != null && message.toLowerCase().contains("account_number");
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .userId(account.getUser() != null ? account.getUser().getUserId() : null)
                .accountNumber(account.getAccountNumber())
                .iban(account.getIban())
                .routingNumber(account.getRoutingNumber())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdDate(account.getCreatedDate())
                .build();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeAccountType(String accountType) {
        String normalized = trim(accountType).toUpperCase();
        if ("SALERY".equals(normalized)) {
            return "SALARY";
        }
        return normalized;
    }
}
