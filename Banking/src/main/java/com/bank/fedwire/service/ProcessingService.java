package com.bank.fedwire.service;

import com.bank.fedwire.dto.ProcessingPipelineDTO;

public interface ProcessingService {

    ProcessingPipelineDTO getProcessingPipeline(Long transactionId);
}
