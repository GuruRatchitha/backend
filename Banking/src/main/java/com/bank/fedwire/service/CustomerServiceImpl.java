package com.bank.fedwire.service;

import com.bank.fedwire.dto.CustomerPageResponse;
import com.bank.fedwire.dto.CustomerRequest;
import com.bank.fedwire.dto.CustomerResponse;
import com.bank.fedwire.dto.AccountResponse;
import com.bank.fedwire.dto.CustomerAccountRequest;
import com.bank.fedwire.dto.CustomerUpdateRequest;
import com.bank.fedwire.entity.Account;
import com.bank.fedwire.entity.DashboardActivity;
import com.bank.fedwire.entity.Role;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.exception.DuplicateAccountNumberException;
import com.bank.fedwire.exception.DuplicateResourceException;
import com.bank.fedwire.exception.ResourceNotFoundException;
import com.bank.fedwire.repository.AccountRepository;
import com.bank.fedwire.repository.DashboardActivityRepository;
import com.bank.fedwire.repository.RoleRepository;
import com.bank.fedwire.repository.UserRepository;
import com.bank.fedwire.util.AccountNumberGenerator;
import com.bank.fedwire.util.IbanGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private static final Long CUSTOMER_ROLE_ID = 2L;
    private static final String DEFAULT_COUNTRY_CODE = "US";
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 20;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final IbanGenerator ibanGenerator;
    private final DashboardActivityRepository dashboardActivityRepository;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Create customer request received with accountType={} initialBalance={}",
                request.getAccountType(), request.getInitialBalance());
        validateUniqueCustomerFields(request, null);

        Role customerRole = roleRepository.findById(CUSTOMER_ROLE_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Customer role not found with id: " + CUSTOMER_ROLE_ID));

        User user = User.builder()
                .userName(trim(request.getFullName()))
                .email(trim(request.getEmail()))
                .password(request.getPassword())
                .phoneNumber(trim(request.getPhoneNumber()))
                .aadharNumber(trim(request.getAadharNumber()))
                .panCardNumber(trim(request.getPanCardNumber()).toUpperCase())
                .address(trim(request.getAddress()))
                .townName(trim(request.getTownName()))
                .countryCode(DEFAULT_COUNTRY_CODE)
                .createdDate(LocalDateTime.now())
                .role(customerRole)
                .build();

        User savedUser = userRepository.saveAndFlush(user);
        log.info("Created user for customer with generated userId={}", savedUser.getUserId());

        Account savedAccount = createAccountForCustomer(savedUser, request);
        savedUser.getAccounts().add(savedAccount);
        logActivity("Customer Created", "Customer " + savedUser.getUserName() + " was created.");
        logActivity("Account Created", "Account " + savedAccount.getAccountNumber() + " was created for customer " + savedUser.getUserName() + ".");

        return toCustomerResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPageResponse getAllCustomers(Pageable pageable) {
        Page<User> customers = userRepository.findByRoleRoleId(CUSTOMER_ROLE_ID, pageable);
        return CustomerPageResponse.builder()
                .content(customers.getContent().stream()
                        .map(this::toCustomerResponse)
                        .toList())
                .totalElements(customers.getTotalElements())
                .totalPages(customers.getTotalPages())
                .currentPage(customers.getNumber())
                .pageSize(customers.getSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return toCustomerResponse(getCustomer(id));
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request) {
        User user = getCustomer(id);
        validateUniqueCustomerFields(request, id);

        updateUserFields(user, request);
        updateAccounts(user, request.getAccounts());
        logActivity("Customer Updated", "Customer " + user.getUserName() + " was updated.");

        return toCustomerResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        userRepository.delete(getCustomer(id));
    }

    private User getCustomer(Long id) {
        User user = userRepository.findWithRoleAndAccountsByUserId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        Role role = user.getRole();
        if (role == null || !CUSTOMER_ROLE_ID.equals(role.getRoleId())) {
            throw new ResourceNotFoundException("Customer not found with id: " + id);
        }
        return user;
    }

    private void validateUniqueCustomerFields(CustomerRequest request, Long existingUserId) {
        String email = trim(request.getEmail());
        String aadharNumber = trim(request.getAadharNumber());
        String panCardNumber = trim(request.getPanCardNumber());

        boolean emailExists = existingUserId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndUserIdNot(email, existingUserId);
        if (emailExists) {
            throw new DuplicateResourceException("Email already exists.");
        }

        boolean aadharExists = existingUserId == null
                ? userRepository.existsByAadharNumber(aadharNumber)
                : userRepository.existsByAadharNumberAndUserIdNot(aadharNumber, existingUserId);
        if (aadharExists) {
            throw new DuplicateResourceException("Aadhar number already exists.");
        }

        boolean panExists = existingUserId == null
                ? userRepository.existsByPanCardNumberIgnoreCase(panCardNumber)
                : userRepository.existsByPanCardNumberIgnoreCaseAndUserIdNot(panCardNumber, existingUserId);
        if (panExists) {
            throw new DuplicateResourceException("PAN card number already exists.");
        }
    }

    private void validateUniqueCustomerFields(CustomerUpdateRequest request, Long existingUserId) {
        String email = trim(request.getEmail());
        String aadhaarNumber = trim(request.getAadhaarNumber());
        String panCardNumber = trim(request.getPanCardNumber());

        if (email != null) {
            boolean emailExists = userRepository.existsByEmailIgnoreCaseAndUserIdNot(email, existingUserId);
            if (emailExists) {
                throw new DuplicateResourceException("Email already exists.");
            }
        }

        if (aadhaarNumber != null) {
            boolean aadharExists = userRepository.existsByAadharNumberAndUserIdNot(aadhaarNumber, existingUserId);
            if (aadharExists) {
                throw new DuplicateResourceException("Aadhaar number already exists.");
            }
        }

        if (panCardNumber != null) {
            boolean panExists = userRepository.existsByPanCardNumberIgnoreCaseAndUserIdNot(panCardNumber, existingUserId);
            if (panExists) {
                throw new DuplicateResourceException("PAN card number already exists.");
            }
        }
    }

    private void updateUserFields(User user, CustomerUpdateRequest request) {
        if (request.getUserName() != null) {
            user.setUserName(trim(request.getUserName()));
        }
        if (request.getEmail() != null) {
            user.setEmail(trim(request.getEmail()));
        }
        if (request.getPassword() != null) {
            user.setPassword(request.getPassword());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(trim(request.getPhoneNumber()));
        }
        if (request.getAadhaarNumber() != null) {
            user.setAadharNumber(trim(request.getAadhaarNumber()));
        }
        if (request.getPanCardNumber() != null) {
            user.setPanCardNumber(trim(request.getPanCardNumber()).toUpperCase());
        }
        if (request.getAddress() != null) {
            user.setAddress(trim(request.getAddress()));
        }
        if (request.getTownName() != null) {
            user.setTownName(trim(request.getTownName()));
        }
        if (request.getCountryCode() != null) {
            user.setCountryCode(trim(request.getCountryCode()).toUpperCase());
        }
    }

    private void updateAccounts(User user, List<CustomerAccountRequest> accountRequests) {
        if (accountRequests == null) {
            return;
        }

        for (CustomerAccountRequest accountRequest : accountRequests) {
            if (accountRequest.getAccountId() == null) {
                Account newAccount = createAccountForCustomer(user, accountRequest);
                user.addAccount(newAccount);
                logActivity("Account Created", "Account " + newAccount.getAccountNumber() + " was created for customer " + user.getUserName() + ".");
            } else {
                Account account = findOwnedAccount(user, accountRequest.getAccountId());
                updateExistingAccount(account, accountRequest);
            }
        }
    }

    private Account findOwnedAccount(User user, Long accountId) {
        return user.getAccounts().stream()
                .filter(account -> Objects.equals(account.getAccountId(), accountId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found for customer. userId=" + user.getUserId() + ", accountId=" + accountId));
    }

    private void updateExistingAccount(Account account, CustomerAccountRequest request) {
        if (request.getBalance() != null) {
            account.setBalance(request.getBalance());
        }
        if (request.getAccountType() != null) {
            account.setAccountType(normalizeAccountType(request.getAccountType()));
        }
        if (request.getStatus() != null) {
            account.setStatus(normalizeStatus(request.getStatus()));
        }
    }

    private Account createAccountForCustomer(User user, CustomerRequest request) {
        Account account = Account.builder()
                .user(user)
                .accountNumber(generateUniqueAccountNumber())
                .accountType(normalizeAccountType(request.getAccountType()))
                .balance(request.getInitialBalance())
                .currency(DEFAULT_CURRENCY)
                .status(DEFAULT_STATUS)
                .createdDate(LocalDateTime.now())
                .build();

        log.info(
                "Account object before saving: userId={} accountType={} initialBalance={} accountNumber={} currency={} status={} createdDate={}",
                user.getUserId(),
                account.getAccountType(),
                account.getBalance(),
                account.getAccountNumber(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedDate());

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
        Account accountWithIban = accountRepository.save(savedAccount);
        log.info(
                "Created account accountId={} userId={} accountType={} balance={} iban={}",
                accountWithIban.getAccountId(),
                user.getUserId(),
                accountWithIban.getAccountType(),
                accountWithIban.getBalance(),
                accountWithIban.getIban());
        return accountWithIban;
    }

    private Account createAccountForCustomer(User user, CustomerAccountRequest request) {
        if (request.getBalance() == null) {
            throw new IllegalArgumentException("Balance is required for a new account.");
        }
        if (trim(request.getAccountType()) == null) {
            throw new IllegalArgumentException("Account type is required for a new account.");
        }

        Account account = Account.builder()
                .user(user)
                .accountNumber(generateUniqueAccountNumber())
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
        return accountRepository.save(savedAccount);
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

    private boolean isAccountNumberConstraintViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return message != null && message.toLowerCase().contains("account_number");
    }

    private CustomerResponse toCustomerResponse(User user) {
        return CustomerResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .aadharNumber(user.getAadharNumber())
                .panCardNumber(user.getPanCardNumber())
                .address(user.getAddress())
                .townName(user.getTownName())
                .countryCode(user.getCountryCode())
                .createdDate(user.getCreatedDate())
                .accounts(toAccountResponses(user.getAccounts()))
                .build();
    }

    private List<AccountResponse> toAccountResponses(List<Account> accounts) {
        if (accounts == null) {
            return List.of();
        }
        return accounts.stream()
                .sorted(Comparator.comparing(Account::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toAccountResponse)
                .toList();
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .userId(account.getUser() != null ? account.getUser().getUserId() : null)
                .accountNumber(account.getAccountNumber())
                .iban(account.getIban())
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

    private String normalizeStatus(String status) {
        return trim(status).toUpperCase();
    }

    private void logActivity(String activity, String description) {
        dashboardActivityRepository.save(DashboardActivity.builder()
                .activity(activity)
                .description(description)
                .employeeName("System")
                .build());
    }
}
