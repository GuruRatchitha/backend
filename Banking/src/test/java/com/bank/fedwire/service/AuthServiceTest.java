package com.bank.fedwire.service;

import com.bank.fedwire.dto.LoginRequest;
import com.bank.fedwire.dto.LoginResponse;
import com.bank.fedwire.entity.Role;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginTrimsCredentialsAndLooksUpEmailIgnoringCase() {
        User user = User.builder()
                .userId(10L)
                .userName("Ravi Kumar")
                .email("ravi.kumar@example.com")
                .password("Password@123")
                .role(Role.builder().roleId(2L).build())
                .build();

        when(userRepository.findByEmailIgnoreCase("RAVI.KUMAR@example.com"))
                .thenReturn(Optional.of(user));

        ResponseEntity<LoginResponse> response = authService.login(LoginRequest.builder()
                .email(" RAVI.KUMAR@example.com ")
                .password(" Password@123 ")
                .build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPassword()).isNull();
        assertThat(response.getBody().getRoleName()).isEqualTo("CUSTOMER");
        assertThat(response.getBody().getMessage()).isEqualTo("Login successful");
        verify(userRepository).findByEmailIgnoreCase("RAVI.KUMAR@example.com");
    }

    @Test
    void loginRejectsBlankCredentials() {
        ResponseEntity<LoginResponse> response = authService.login(LoginRequest.builder()
                .email(" ")
                .password("Password@123")
                .build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
    }
}
