package com.bank.fedwire.service;

import com.bank.fedwire.dto.ProcessingPipelineDTO;
import com.bank.fedwire.dto.ProcessingStatus;
import com.bank.fedwire.dto.ProcessingStepDTO;
import com.bank.fedwire.repository.ProcessingPipelineProjection;
import com.bank.fedwire.repository.ProcessingPipelineRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessingServiceImplTest {

    private final ProcessingPipelineRepository repository = mock(ProcessingPipelineRepository.class);
    private final ProcessingServiceImpl service = new ProcessingServiceImpl(repository);

    @Test
    void getProcessingPipelineBuildsAcceptedTimeline() {
        LocalDateTime generatedAt = LocalDateTime.of(2026, 6, 30, 11, 22, 15);
        LocalDateTime sentAt = generatedAt.plusSeconds(3);
        LocalDateTime receivedAt = sentAt.plusSeconds(2);
        LocalDateTime settledAt = receivedAt.plusSeconds(5);

        TestPipelineProjection projection = new TestPipelineProjection();
        projection.transactionId = 47L;
        projection.transactionStatus = "COMPLETED";
        projection.pacs008Id = 1L;
        projection.pacs008XmlPayload = "<xml/>";
        projection.pacs008CreatedDate = generatedAt;
        projection.pacs008SentAt = sentAt;
        projection.pacs002Id = 2L;
        projection.pacs002TransactionStatus = "ACSC";
        projection.pacs002ReceivedTimestamp = receivedAt;
        projection.beneficiarySettlementAt = settledAt;
        when(repository.findPipelineByTransactionId(47L)).thenReturn(Optional.of(projection));

        ProcessingPipelineDTO response = service.getProcessingPipeline(47L);

        assertEquals(47L, response.transactionId());
        assertEquals("COMPLETED", response.status());
        assertEquals(6, response.steps().size());
        assertStep(response.steps().get(0), "FORMAT_VALIDATION", ProcessingStatus.COMPLETED, null, null);
        assertStep(response.steps().get(1), "PACS008_SENT", ProcessingStatus.COMPLETED, null, null);
        assertStep(response.steps().get(2), "RESPONSE_RECEIVED", ProcessingStatus.COMPLETED, "PACS002", null);
        assertStep(response.steps().get(3), "TRANSACTION_DECISION", ProcessingStatus.COMPLETED, "PACS002", "ACCEPTED");
        assertEquals("ACSC", response.steps().get(3).txStatus());
        assertStep(response.steps().get(4), "SETTLEMENT", ProcessingStatus.COMPLETED, null, "ACCEPTED");
        assertEquals("BENEFICIARY", response.steps().get(4).direction());
        assertEquals("Amount credited to beneficiary", response.steps().get(4).message());
        assertStep(response.steps().get(5), "PROCESS_COMPLETED", ProcessingStatus.COMPLETED, null, null);
    }

    @Test
    void getProcessingPipelineKeepsDecisionPendingForAdmi002() {
        LocalDateTime receivedAt = LocalDateTime.of(2026, 6, 30, 11, 22, 20);

        TestPipelineProjection projection = new TestPipelineProjection();
        projection.transactionId = 48L;
        projection.transactionStatus = "REJECTED";
        projection.pacs008Id = 1L;
        projection.pacs008XmlPayload = "<xml/>";
        projection.pacs008CreatedDate = receivedAt.minusSeconds(5);
        projection.pacs008SentAt = receivedAt.minusSeconds(2);
        projection.admi002Id = 3L;
        projection.admi002ReceivedTimestamp = receivedAt;
        when(repository.findPipelineByTransactionId(48L)).thenReturn(Optional.of(projection));

        ProcessingPipelineDTO response = service.getProcessingPipeline(48L);

        assertEquals("REJECTED", response.status());
        assertStep(response.steps().get(2), "RESPONSE_RECEIVED", ProcessingStatus.COMPLETED, "ADMI002", null);
        assertStep(response.steps().get(3), "TRANSACTION_DECISION", ProcessingStatus.PENDING, "ADMI002", "PENDING");
        assertStep(response.steps().get(4), "SETTLEMENT", ProcessingStatus.NOT_APPLICABLE, "ADMI002", "PENDING");
        assertStep(response.steps().get(5), "PROCESS_COMPLETED", ProcessingStatus.NOT_APPLICABLE, "ADMI002", "PENDING");
    }

    @Test
    void getProcessingPipelineReturnsPendingStepsWhenNoMessagesExist() {
        TestPipelineProjection projection = new TestPipelineProjection();
        projection.transactionId = 49L;
        projection.transactionStatus = "PENDING";
        when(repository.findPipelineByTransactionId(49L)).thenReturn(Optional.of(projection));

        ProcessingPipelineDTO response = service.getProcessingPipeline(49L);

        assertEquals("PENDING", response.status());
        for (ProcessingStepDTO step : response.steps()) {
            assertEquals(ProcessingStatus.PENDING, step.status());
            assertNull(step.timestamp());
        }
    }

    private void assertStep(ProcessingStepDTO step,
                            String expectedStep,
                            ProcessingStatus expectedStatus,
                            String expectedResponseType,
                            String expectedDecision) {
        assertEquals(expectedStep, step.step());
        assertEquals(expectedStatus, step.status());
        assertEquals(expectedResponseType, step.responseType());
        assertEquals(expectedDecision, step.decision());
    }

    private static class TestPipelineProjection implements ProcessingPipelineProjection {

        private Long transactionId;
        private String transactionStatus;
        private LocalDateTime transactionDateTime;
        private Long pacs008Id;
        private String pacs008XmlPayload;
        private LocalDateTime pacs008CreatedDate;
        private LocalDateTime pacs008SentAt;
        private String pacs008MessageId;
        private Long pacs002Id;
        private String pacs002TransactionStatus;
        private LocalDateTime pacs002ReceivedTimestamp;
        private Long admi002Id;
        private LocalDateTime admi002ReceivedTimestamp;
        private LocalDateTime beneficiarySettlementAt;
        private LocalDateTime returnSettlementAt;

        @Override
        public Long getTransactionId() {
            return transactionId;
        }

        @Override
        public String getTransactionStatus() {
            return transactionStatus;
        }

        @Override
        public LocalDateTime getTransactionDateTime() {
            return transactionDateTime;
        }

        @Override
        public Long getPacs008Id() {
            return pacs008Id;
        }

        @Override
        public String getPacs008XmlPayload() {
            return pacs008XmlPayload;
        }

        @Override
        public LocalDateTime getPacs008CreatedDate() {
            return pacs008CreatedDate;
        }

        @Override
        public LocalDateTime getPacs008SentAt() {
            return pacs008SentAt;
        }

        @Override
        public String getPacs008MessageId() {
            return pacs008MessageId;
        }

        @Override
        public Long getPacs002Id() {
            return pacs002Id;
        }

        @Override
        public String getPacs002TransactionStatus() {
            return pacs002TransactionStatus;
        }

        @Override
        public LocalDateTime getPacs002ReceivedTimestamp() {
            return pacs002ReceivedTimestamp;
        }

        @Override
        public Long getAdmi002Id() {
            return admi002Id;
        }

        @Override
        public LocalDateTime getAdmi002ReceivedTimestamp() {
            return admi002ReceivedTimestamp;
        }

        @Override
        public LocalDateTime getBeneficiarySettlementAt() {
            return beneficiarySettlementAt;
        }

        @Override
        public LocalDateTime getReturnSettlementAt() {
            return returnSettlementAt;
        }
    }
}
