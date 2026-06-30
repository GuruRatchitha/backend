package com.bank.fedwire.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
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
public class CustomerAccountRequest {

    private Long accountId;

    @JsonAlias("account_type")
    @Pattern(
            regexp = "(?i)^(savings|current|salary|salery)$",
            message = "Account type must be SAVINGS, CURRENT, or SALARY."
    )
    private String accountType;

    @JsonAlias({"initialBalance", "initial_balance"})
    @DecimalMin(value = "0.00", inclusive = true, message = "Balance must be zero or greater.")
    private BigDecimal balance;

    @Pattern(
            regexp = "(?i)^(active|inactive|closed|frozen)$",
            message = "Status must be ACTIVE, INACTIVE, CLOSED, or FROZEN."
    )
    private String status;
}
