package com.bank.fedwire.service;

import com.bank.fedwire.dto.UserProfileResponse;
import com.bank.fedwire.dto.UserProfileUpdateRequest;
import com.bank.fedwire.entity.Role;
import com.bank.fedwire.entity.User;
import com.bank.fedwire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String MASKED_PASSWORD = "********";

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = getUserById(userId);
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = getUserById(userId);

        user.setAadharNumber(request.getAadharNumber());
        user.setAddress(request.getAddress());
        user.setPanCardNumber(request.getPanCardNumber());
        user.setPassword(request.getPassword());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setUserName(request.getUsername());

        return toProfileResponse(userRepository.save(user));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfileResponse toProfileResponse(User user) {
        Role role = user.getRole();

        return UserProfileResponse.builder()
                .createdDate(user.getCreatedDate())
                .roleId(role != null ? role.getRoleId() : null)
                .userId(user.getUserId())
                .email(user.getEmail())
                .aadharNumber(user.getAadharNumber())
                .address(user.getAddress())
                .panCardNumber(user.getPanCardNumber())
                .password(MASKED_PASSWORD)
                .phoneNumber(user.getPhoneNumber())
                .username(user.getUserName())
                .build();
    }
}
