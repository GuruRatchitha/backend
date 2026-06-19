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
public class BeneficiaryResponse {

    private Long userId;

    private String beneficiaryName;

    private String townName;

    private String countryCode;

    private String accountNumber;

    private String routingNumber;

    private LocalDateTime createdDate;

    private String status;
}
