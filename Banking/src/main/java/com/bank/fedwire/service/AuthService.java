package com.bank.fedwire.service;

import com.bank.fedwire.dto.LoginRequest;
import com.bank.fedwire.dto.LoginResponse;
import com.bank.fedwire.entity.Role;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ResponseEntity<LoginResponse> login(LoginRequest request) {

        String email = normalize(request != null ? request.getEmail() : null);
        String password = normalize(request != null ? request.getPassword() : null);

        if (email == null || password == null) {
            return invalidLoginResponse();
        }

        return userRepository.findByEmailIgnoreCase(email)
                .filter(user -> passwordMatches(password, user.getPassword()))
                .map(user -> ResponseEntity.ok(toLoginResponse(user)))
                .orElseGet(this::invalidLoginResponse);
    }

    private boolean passwordMatches(String requestPassword, String storedPassword) {
        return storedPassword != null && storedPassword.trim().equals(requestPassword);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LoginResponse toLoginResponse(User user) {
        Role role = user.getRole();
        Long roleId = role != null ? role.getRoleId() : null;

        return LoginResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .aadharNumber(user.getAadharNumber())
                .panCardNumber(user.getPanCardNumber())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .createdDate(user.getCreatedDate())
                .roleId(roleId)
                .roleName(resolveRoleName(role))
                .message("Login successful")
                .build();
    }

    private ResponseEntity<LoginResponse> invalidLoginResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(LoginResponse.builder()
                        .message("Invalid email or password")
                        .build());
    }

    private String resolveRoleName(Role role) {
        String roleName = role != null ? normalize(role.getRoleName()) : null;
        if (roleName != null) {
            return roleName.toUpperCase();
        }

        Long roleId = role != null ? role.getRoleId() : null;
        if (Long.valueOf(1L).equals(roleId)) {
            return "EMPLOYEE";
        }

        if (Long.valueOf(2L).equals(roleId)) {
            return "CUSTOMER";
        }

        return null;
    }
}
