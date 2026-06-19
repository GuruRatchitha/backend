package com.bank.fedwire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private Long userId;

    private String userName;

    private String email;

    // Sends the database role_id back to the client after successful login.
    private Long roleId;

    // Sends the application role name resolved from role_id without changing the users table.
    private String roleName;

    private String message;
}
