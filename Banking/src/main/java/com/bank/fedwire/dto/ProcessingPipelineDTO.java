package com.bank.fedwire.dto;

import java.util.List;

public record ProcessingPipelineDTO(
        Long transactionId,
        String status,
        List<ProcessingStepDTO> steps
) {
}
