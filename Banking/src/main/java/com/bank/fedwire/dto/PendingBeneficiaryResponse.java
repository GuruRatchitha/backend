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
public class PendingBeneficiaryResponse {

    private Long beneficiaryId;

    private String customerName;

    private String beneficiaryName;

    private String bankName;

    private String accountNumber;

    private String status;

    private LocalDateTime requestedDate;
}
