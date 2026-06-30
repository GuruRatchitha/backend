package com.bank.fedwire.service;

import com.bank.fedwire.dto.ProcessingPipelineDTO;
import com.bank.fedwire.dto.ProcessingStatus;
import com.bank.fedwire.dto.ProcessingStepDTO;
import com.bank.fedwire.repository.ProcessingPipelineProjection;
import com.bank.fedwire.repository.ProcessingPipelineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProcessingServiceImpl implements ProcessingService {

    private static final String STEP_FORMAT_VALIDATION = "FORMAT_VALIDATION";
    private static final String STEP_PACS008_SENT = "PACS008_SENT";
    private static final String STEP_RESPONSE_RECEIVED = "RESPONSE_RECEIVED";
    private static final String STEP_TRANSACTION_DECISION = "TRANSACTION_DECISION";
    private static final String STEP_SETTLEMENT = "SETTLEMENT";
    private static final String STEP_PROCESS_COMPLETED = "PROCESS_COMPLETED";

    private static final String RESPONSE_PACS002 = "PACS002";
    private static final String RESPONSE_ADMI002 = "ADMI002";

    private static final String DECISION_ACCEPTED = "ACCEPTED";
    private static final String DECISION_REJECTED = "REJECTED";
    private static final String DECISION_PENDING = "PENDING";

    private final ProcessingPipelineRepository processingPipelineRepository;

    @Override
    @Transactional(readOnly = true)
    public ProcessingPipelineDTO getProcessingPipeline(Long transactionId) {
        ProcessingPipelineProjection pipeline = processingPipelineRepository.findPipelineByTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transaction not found for transactionId " + transactionId));

        ResponseInfo responseInfo = resolveResponseInfo(pipeline);
        DecisionInfo decisionInfo = resolveDecisionInfo(responseInfo);
        SettlementInfo settlementInfo = resolveSettlementInfo(pipeline, decisionInfo);

        List<ProcessingStepDTO> steps = new ArrayList<>();
        steps.add(formatValidationStep(pipeline));
        steps.add(pacs008SentStep(pipeline));
        steps.add(responseReceivedStep(responseInfo));
        steps.add(transactionDecisionStep(responseInfo, decisionInfo));
        steps.add(settlementStep(responseInfo, decisionInfo, settlementInfo));
        steps.add(processCompletedStep(responseInfo, settlementInfo));

        return new ProcessingPipelineDTO(pipeline.getTransactionId(), pipeline.getTransactionStatus(), steps);
    }

    private ProcessingStepDTO formatValidationStep(ProcessingPipelineProjection pipeline) {
        if (pipeline.getPacs008Id() == null) {
            return step(STEP_FORMAT_VALIDATION, ProcessingStatus.PENDING, null, null, null, null,
                    null, "PACS.008 generation pending");
        }
        if (isBlank(pipeline.getPacs008XmlPayload())) {
            return step(STEP_FORMAT_VALIDATION, ProcessingStatus.FAILED, null, null, null, null,
                    pipeline.getPacs008CreatedDate(), "PACS.008 generation failed");
        }
        return step(STEP_FORMAT_VALIDATION, ProcessingStatus.COMPLETED, null, null, null, null,
                pipeline.getPacs008CreatedDate(), "PACS.008 generated successfully");
    }

    private ProcessingStepDTO pacs008SentStep(ProcessingPipelineProjection pipeline) {
        if (pipeline.getPacs008SentAt() != null) {
            return step(STEP_PACS008_SENT, ProcessingStatus.COMPLETED, null, null, null, null,
                    pipeline.getPacs008SentAt(), "Submitted to PayApt");
        }
        if (pipeline.getPacs008Id() != null && isBlank(pipeline.getPacs008XmlPayload())) {
            return step(STEP_PACS008_SENT, ProcessingStatus.FAILED, null, null, null, null,
                    pipeline.getPacs008CreatedDate(), "PACS.008 was not submitted because XML generation failed");
        }
        return step(STEP_PACS008_SENT, ProcessingStatus.PENDING, null, null, null, null,
                null, "PACS.008 submission pending");
    }

    private ProcessingStepDTO responseReceivedStep(ResponseInfo responseInfo) {
        if (responseInfo.responseType() == null) {
            return step(STEP_RESPONSE_RECEIVED, ProcessingStatus.PENDING, null, null, null, null,
                    null, "Response not received yet");
        }
        String message = RESPONSE_ADMI002.equals(responseInfo.responseType())
                ? "ADMI.002 received"
                : "PACS.002 received";
        return step(STEP_RESPONSE_RECEIVED, ProcessingStatus.COMPLETED, responseInfo.responseType(), null, null, null,
                responseInfo.receivedAt(), message);
    }

    private ProcessingStepDTO transactionDecisionStep(ResponseInfo responseInfo, DecisionInfo decisionInfo) {
        if (responseInfo.responseType() == null) {
            return step(STEP_TRANSACTION_DECISION, ProcessingStatus.PENDING, null, DECISION_PENDING, null, null,
                    null, "Transaction decision pending");
        }
        if (RESPONSE_ADMI002.equals(responseInfo.responseType())) {
            return step(STEP_TRANSACTION_DECISION, ProcessingStatus.PENDING, RESPONSE_ADMI002, DECISION_PENDING, null, null,
                    responseInfo.receivedAt(), "Transaction decision pending because ADMI.002 was received");
        }
        return switch (decisionInfo.decision()) {
            case DECISION_ACCEPTED -> step(STEP_TRANSACTION_DECISION, ProcessingStatus.COMPLETED, RESPONSE_PACS002,
                    decisionInfo.decision(), decisionInfo.txStatus(), null, responseInfo.receivedAt(), "Transaction accepted");
            case DECISION_REJECTED -> step(STEP_TRANSACTION_DECISION, ProcessingStatus.COMPLETED, RESPONSE_PACS002,
                    decisionInfo.decision(), decisionInfo.txStatus(), null, responseInfo.receivedAt(), "Transaction rejected");
            default -> step(STEP_TRANSACTION_DECISION, ProcessingStatus.PENDING, RESPONSE_PACS002,
                    DECISION_PENDING, decisionInfo.txStatus(), null, responseInfo.receivedAt(), "Transaction decision pending");
        };
    }

    private ProcessingStepDTO settlementStep(ResponseInfo responseInfo,
                                             DecisionInfo decisionInfo,
                                             SettlementInfo settlementInfo) {
        if (RESPONSE_ADMI002.equals(responseInfo.responseType())) {
            return step(STEP_SETTLEMENT, ProcessingStatus.NOT_APPLICABLE, RESPONSE_ADMI002, DECISION_PENDING, null, null,
                    null, "Settlement not applicable because syntax validation failed");
        }
        if (settlementInfo.completedAt() != null) {
            return step(STEP_SETTLEMENT, ProcessingStatus.COMPLETED, null, decisionInfo.decision(), decisionInfo.txStatus(),
                    settlementInfo.direction(), settlementInfo.completedAt(), settlementInfo.message());
        }
        if (DECISION_ACCEPTED.equals(decisionInfo.decision())) {
            return step(STEP_SETTLEMENT, ProcessingStatus.PENDING, null, decisionInfo.decision(), decisionInfo.txStatus(),
                    "BENEFICIARY", null, "Beneficiary settlement pending");
        }
        if (DECISION_REJECTED.equals(decisionInfo.decision())) {
            return step(STEP_SETTLEMENT, ProcessingStatus.PENDING, null, decisionInfo.decision(), decisionInfo.txStatus(),
                    "SENDER", null, "Sender return pending");
        }
        return step(STEP_SETTLEMENT, ProcessingStatus.PENDING, null, decisionInfo.decision(), decisionInfo.txStatus(), null,
                null, "Settlement pending");
    }

    private ProcessingStepDTO processCompletedStep(ResponseInfo responseInfo, SettlementInfo settlementInfo) {
        if (RESPONSE_ADMI002.equals(responseInfo.responseType())) {
            return step(STEP_PROCESS_COMPLETED, ProcessingStatus.NOT_APPLICABLE, RESPONSE_ADMI002, DECISION_PENDING, null, null,
                    null, "Workflow stopped before transaction processing");
        }
        if (settlementInfo.completedAt() != null) {
            return step(STEP_PROCESS_COMPLETED, ProcessingStatus.COMPLETED, null, null, null, null,
                    settlementInfo.completedAt(), "Workflow completed");
        }
        return step(STEP_PROCESS_COMPLETED, ProcessingStatus.PENDING, null, null, null, null,
                null, "Workflow completion pending");
    }

    private ResponseInfo resolveResponseInfo(ProcessingPipelineProjection pipeline) {
        LocalDateTime pacs002ReceivedAt = pipeline.getPacs002ReceivedTimestamp();
        LocalDateTime admi002ReceivedAt = pipeline.getAdmi002ReceivedTimestamp();

        if (pacs002ReceivedAt == null && admi002ReceivedAt == null) {
            return new ResponseInfo(null, null, null);
        }
        if (admi002ReceivedAt != null && (pacs002ReceivedAt == null || admi002ReceivedAt.isAfter(pacs002ReceivedAt))) {
            return new ResponseInfo(RESPONSE_ADMI002, admi002ReceivedAt, null);
        }
        return new ResponseInfo(RESPONSE_PACS002, pacs002ReceivedAt, pipeline.getPacs002TransactionStatus());
    }

    private DecisionInfo resolveDecisionInfo(ResponseInfo responseInfo) {
        if (!RESPONSE_PACS002.equals(responseInfo.responseType())) {
            return new DecisionInfo(DECISION_PENDING, null);
        }

        String txStatus = normalize(responseInfo.txStatus());
        if ("ACSC".equals(txStatus) || "ACCP".equals(txStatus)) {
            return new DecisionInfo(DECISION_ACCEPTED, txStatus);
        }
        if ("RJCT".equals(txStatus)) {
            return new DecisionInfo(DECISION_REJECTED, txStatus);
        }
        return new DecisionInfo(DECISION_PENDING, txStatus);
    }

    private SettlementInfo resolveSettlementInfo(ProcessingPipelineProjection pipeline, DecisionInfo decisionInfo) {
        if (DECISION_ACCEPTED.equals(decisionInfo.decision()) && pipeline.getBeneficiarySettlementAt() != null) {
            return new SettlementInfo("BENEFICIARY", pipeline.getBeneficiarySettlementAt(), "Amount credited to beneficiary");
        }
        if (DECISION_REJECTED.equals(decisionInfo.decision()) && pipeline.getReturnSettlementAt() != null) {
            return new SettlementInfo("SENDER", pipeline.getReturnSettlementAt(), "Amount returned to sender");
        }
        return new SettlementInfo(null, null, null);
    }

    private ProcessingStepDTO step(String step,
                                   ProcessingStatus status,
                                   String responseType,
                                   String decision,
                                   String txStatus,
                                   String direction,
                                   LocalDateTime timestamp,
                                   String message) {
        return new ProcessingStepDTO(step, status, responseType, decision, txStatus, direction, timestamp, message);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ResponseInfo(String responseType, LocalDateTime receivedAt, String txStatus) {
    }

    private record DecisionInfo(String decision, String txStatus) {
    }

    private record SettlementInfo(String direction, LocalDateTime completedAt, String message) {
    }
}
