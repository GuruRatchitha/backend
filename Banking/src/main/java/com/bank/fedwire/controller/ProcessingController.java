package com.bank.fedwire.controller;

import com.bank.fedwire.dto.ProcessingPipelineDTO;
import com.bank.fedwire.service.ProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee/transactions")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
@RequiredArgsConstructor
public class ProcessingController {

    private final ProcessingService processingService;

    @GetMapping("/{transactionId}/processing-pipeline")
    public ResponseEntity<ProcessingPipelineDTO> getProcessingPipeline(@PathVariable Long transactionId) {
        return ResponseEntity.ok(processingService.getProcessingPipeline(transactionId));
    }
}
