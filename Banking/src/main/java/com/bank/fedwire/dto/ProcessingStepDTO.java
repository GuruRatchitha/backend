package com.bank.fedwire.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProcessingStepDTO(
        String step,
        ProcessingStatus status,
        String responseType,
        String decision,
        String txStatus,
        String direction,
        LocalDateTime timestamp,
        String message
) {
}
