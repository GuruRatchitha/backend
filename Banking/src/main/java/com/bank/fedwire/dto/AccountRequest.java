package com.bank.fedwire.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountRequest {

    @NotNull(message = "User id is required.")
    private Long userId;

    @NotBlank(message = "Account type is required.")
    @Pattern(
            regexp = "(?i)^(savings|current|salary|salery)$",
            message = "Account type must be SAVINGS, CURRENT, or SALARY."
    )
    private String accountType;

    @NotNull(message = "Balance is required.")
    @DecimalMin(value = "100.00", inclusive = true, message = "Balance must be at least 100.")
    private BigDecimal balance;
}
