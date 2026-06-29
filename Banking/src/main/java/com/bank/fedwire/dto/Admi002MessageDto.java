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
public class Admi002MessageDto {

    private String messageType;

    private String businessMessageId;

    private String originalReference;

    private String rejectReasonCode;

    private String rejectReasonDescription;

    private LocalDateTime rejectionDateTime;

    private String xmlPayload;
}
