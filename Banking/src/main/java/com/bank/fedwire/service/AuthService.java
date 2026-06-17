package com.bank.fedwire.service;

import com.bank.fedwire.dto.LoginRequest;
import com.bank.fedwire.dto.LoginResponse;
import com.bank.fedwire.entity.Role;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<LoginResponse> login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            return Optional.empty();
        }

        return userRepository.findByEmailAndPassword(request.getEmail(), request.getPassword())
                .map(this::toLoginResponse);
    }

    private LoginResponse toLoginResponse(User user) {
        Role role = user.getRole();

        return LoginResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .roleId(role != null ? role.getRoleId() : null)
                .roleName(role != null ? role.getRoleName() : null)
                .message("Login successful")
                .build();
    }
}
