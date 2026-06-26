package com.bank.fedwire.config;

import com.bank.fedwire.entity.Role;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.RoleRepository;
import com.bank.fedwire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role employeeRole = findOrCreateRole("EMPLOYEE");
        Role customerRole = findOrCreateRole("CUSTOMER");

        createUserIfMissing(
                "employee@example.com",
                "Employee User",
                "Password@123",
                "9000000001",
                "111122223333",
                "ABCDE1234F",
                "100 Operations Street",
                "Fountain Hills",
                employeeRole);

        createUserIfMissing(
                "customer@example.com",
                "Customer User",
                "Password@123",
                "9000000002",
                "444455556666",
                "FGHIJ5678K",
                "200 Main Street",
                "Fountain Hills",
                customerRole);
    }

    private Role findOrCreateRole(String roleName) {
        return roleRepository.findByRoleNameIgnoreCase(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName(roleName)
                        .build()));
    }

    private void createUserIfMissing(
            String email,
            String userName,
            String password,
            String phoneNumber,
            String aadharNumber,
            String panCardNumber,
            String address,
            String townName,
            Role role) {
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }

        userRepository.save(User.builder()
                .userName(userName)
                .email(email)
                .password(password)
                .phoneNumber(phoneNumber)
                .aadharNumber(aadharNumber)
                .panCardNumber(panCardNumber)
                .address(address)
                .countryCode("US")
                .townName(townName)
                .createdDate(LocalDateTime.now())
                .role(role)
                .build());
    }
}
