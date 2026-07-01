package com.bank.fedwire.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerUpdateRequest {

    @JsonAlias({"userName", "fullName", "name"})
    private String userName;

    @Email(message = "Email must be valid.")
    private String email;

    private String password;

    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits.")
    private String phoneNumber;

    @JsonAlias({"aadharNumber", "aadhaarNumber"})
    @Pattern(regexp = "\\d{12}", message = "Aadhaar number must be exactly 12 digits.")
    private String aadhaarNumber;

    @Size(min = 10, max = 10, message = "PAN card number must be exactly 10 characters.")
    private String panCardNumber;

    private String address;

    @Size(max = 35, message = "Town name must be 35 characters or less.")
    private String townName;

    @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters.")
    private String countryCode;

    @Valid
    private List<CustomerAccountRequest> accounts;
}
