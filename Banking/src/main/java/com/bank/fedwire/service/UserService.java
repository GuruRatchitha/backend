package com.bank.fedwire.service;

import com.bank.fedwire.dto.UserProfileResponse;
import com.bank.fedwire.dto.UserProfileUpdateRequest;

public interface UserService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request);
}
