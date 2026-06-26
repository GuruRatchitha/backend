package com.bank.fedwire.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private Long userId;

    private String userName;

    private String email;

    private String phoneNumber;

    @JsonProperty("aadhaarNumber")
    private String aadharNumber;

    private String panCardNumber;

    private String address;

    private String townName;

    private String countryCode;

    private LocalDateTime createdDate;

    private List<AccountResponse> accounts;
}
