package com.bank.fedwire.listener;

import com.bank.fedwire.event.Pacs008ApprovedEvent;
import com.bank.fedwire.service.SnsPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class Pacs008ApprovedEventListener {

    private final SnsPublisherService snsPublisherService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApproved(Pacs008ApprovedEvent event) {
        try {
            snsPublisherService.publishIfNeeded(event.transactionId());
        } catch (Exception ex) {
            log.error("Failed to publish PACS008 to SNS for transactionId={}", event.transactionId(), ex);
        }
    }
}
