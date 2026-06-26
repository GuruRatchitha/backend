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
public class RecentCustomerResponse {

    private Long userId;

    private String userName;

    private String email;

    private String phoneNumber;

    private LocalDateTime createdDate;
}
