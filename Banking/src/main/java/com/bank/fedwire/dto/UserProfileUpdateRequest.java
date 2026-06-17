package com.bank.fedwire.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    private String aadharNumber;

    private String address;

    private String panCardNumber;

    private String password;

    private String phoneNumber;

    private String username;
}
