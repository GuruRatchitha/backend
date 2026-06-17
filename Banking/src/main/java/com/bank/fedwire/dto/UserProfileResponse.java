package com.bank.fedwire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private LocalDateTime createdDate;

    private Long roleId;

    private Long userId;

    private String email;

    private String aadharNumber;

    private String address;

    private String panCardNumber;

    private String password;

    private String phoneNumber;

    private String username;
}
