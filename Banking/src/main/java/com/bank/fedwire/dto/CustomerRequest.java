package com.bank.fedwire.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {

    @JsonAlias({"userName", "name"})
    @NotBlank(message = "Full name is required.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    private String email;

    @NotBlank(message = "Password is required.")
    private String password;

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits.")
    private String phoneNumber;

    @JsonAlias("aadhaarNumber")
    @NotBlank(message = "Aadhar number is required.")
    @Pattern(regexp = "\\d{12}", message = "Aadhar number must be exactly 12 digits.")
    private String aadharNumber;

    @NotBlank(message = "PAN card number is required.")
    @Size(min = 10, max = 10, message = "PAN card number must be exactly 10 characters.")
    private String panCardNumber;

    @NotBlank(message = "Address is required.")
    private String address;

    @NotBlank(message = "Town name is required.")
    private String townName;

    @JsonAlias("account_type")
    @Pattern(
            regexp = "(?i)^(savings|current|salary|salery)$",
            message = "Account type must be SAVINGS, CURRENT, or SALARY."
    )
    private String accountType;

    @JsonAlias("initial_balance")
    @DecimalMin(value = "100.00", inclusive = true, message = "Initial balance must be at least 100.")
    private BigDecimal initialBalance;

    @Valid
    private List<CustomerAccountRequest> accounts;
}
