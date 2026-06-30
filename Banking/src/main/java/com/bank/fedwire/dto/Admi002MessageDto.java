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

    private String messageId;

    private String originalMessageId;

    private String businessMessageId;

    private String relatedMessageId;

    private String originalReference;

    private String errorCode;

    private String errorDescription;

    private String severity;

    private LocalDateTime creationDateTime;

    private String rejectReasonCode;

    private String rejectReasonDescription;

    private LocalDateTime rejectionDateTime;

    private String xmlPayload;
}
