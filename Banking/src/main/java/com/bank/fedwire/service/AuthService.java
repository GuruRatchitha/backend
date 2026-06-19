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
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            return invalidLoginResponse();
        }

        // Keep the existing email/password validation query and only enrich the successful response.
        return userRepository.findByEmailAndPassword(request.getEmail(), request.getPassword())
                .map(user -> ResponseEntity.ok(toLoginResponse(user)))
                .orElseGet(this::invalidLoginResponse);
    }

    private LoginResponse toLoginResponse(User user) {
        Role role = user.getRole();
        Long roleId = role != null ? role.getRoleId() : null;

        return LoginResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .password(user.getPassword())
                .aadharNumber(user.getAadharNumber())
                .panCardNumber(user.getPanCardNumber())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .createdDate(user.getCreatedDate())
                .roleId(roleId)
                .roleName(resolveRoleName(roleId))
                .message("Login successful")
                .build();
    }

    private ResponseEntity<LoginResponse> invalidLoginResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(LoginResponse.builder()
                        .message("Invalid email or password")
                        .build());
    }

    private String resolveRoleName(Long roleId) {
        // Translate the stored role_id values into the role names expected by the login client.
        if (Long.valueOf(1L).equals(roleId)) {
            return "EMPLOYEE";
        }
        if (Long.valueOf(2L).equals(roleId)) {
            return "CUSTOMER";
        }
        return null;
    }
}
