package com.bank.fedwire.listener;

import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.repository.PACS008Repository;
import com.bank.fedwire.service.SnsPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "aws", name = "messaging-enabled", havingValue = "true")
public class Pacs008RetryScheduler {

    private final PACS008Repository pacs008Repository;
    private final SnsPublisherService snsPublisherService;

    @Scheduled(fixedDelayString = "${aws.sqs.pacs008-retry-delay-ms:30000}")
    public void retryUnpublishedMessages() {
        for (PACS008 pacs008 : pacs008Repository.findByXmlPayloadIsNotNullAndSqsPublishedAtIsNull()) {
            try {
                snsPublisherService.publishIfNeeded(pacs008.getTransactionId());
            } catch (Exception ex) {
                log.error("Retry publish failed for transactionId={}", pacs008.getTransactionId(), ex);
            }
        }
    }
}
