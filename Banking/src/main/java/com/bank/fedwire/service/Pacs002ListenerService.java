package com.bank.fedwire.service;

import com.bank.fedwire.config.AwsProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "aws", name = "messaging-enabled", havingValue = "true")
public class Pacs002ListenerService {

    private static final AtomicLong POLL_INVOCATION_COUNT = new AtomicLong(0L);

    private final SqsClient sqsClient;
    private final AwsProperties awsProperties;
    private final Pacs002ProcessingService pacs002ProcessingService;
    private final Admi002ProcessingService admi002ProcessingService;
    private final InboundSqsMessageSupport inboundSqsMessageSupport;

    @PostConstruct
    public void logStartup() {
        log.info("Inbound message listener initialized queueUrl={}, region={}, accessKeyLoaded={}, secretKeyLoaded={}",
                awsProperties.getPacs002QueueUrl(),
                awsProperties.getRegion(),
                isLoaded(awsProperties.getAccessKey()),
                isLoaded(awsProperties.getSecretKey()));
    }

    @Scheduled(fixedDelayString = "${aws.pacs002-poll-delay-ms:5000}")
    public void pollQueue() {
        long invocation = POLL_INVOCATION_COUNT.incrementAndGet();
        String queueUrl = awsProperties.getPacs002QueueUrl();
        log.info("pollQueue invoked invocation={}, timestamp={}, queueUrl={}, region={}",
                invocation, Instant.now(), queueUrl, awsProperties.getRegion());
        if (queueUrl == null || queueUrl.isBlank()) {
            log.warn("Inbound queue URL is not configured, skipping poll");
            return;
        }

        try {
            log.info("Calling receiveMessage invocation={}, queueUrl={}, maxMessages={}, waitTimeSeconds={}",
                    invocation, queueUrl, 10, 20);
            ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(20)
                    .build());

            log.info("receiveMessage completed invocation={}, queueUrl={}, messageCount={}",
                    invocation, queueUrl, response.messages().size());
            if (response.messages().isEmpty()) {
                log.info("No inbound messages received from queueUrl={} on invocation={}", queueUrl, invocation);
            }

            for (Message message : response.messages()) {
                log.info("Dispatching inbound SQS message invocation={}, messageId={}, bodyLength={}",
                        invocation, message.messageId(), message.body() != null ? message.body().length() : 0);
                handleMessage(queueUrl, message);
            }
        } catch (Exception ex) {
            log.error("Failed to poll inbound queue {}", queueUrl, ex);
        }
    }

    private void handleMessage(String queueUrl, Message message) {
        String rawBody = message.body();
        String payload = inboundSqsMessageSupport.extractXmlPayload(rawBody);
        InboundSqsMessageSupport.InboundMessageMetadata metadata = inboundSqsMessageSupport.extractMetadata(payload);
        if (metadata.messageType() == null) {
            metadata = inboundSqsMessageSupport.extractMetadata(rawBody);
        }
        if (metadata.messageType() == null) {
            metadata = inboundSqsMessageSupport.detectMessageTypeByScan(payload);
        }
        if (metadata.messageType() == null) {
            metadata = inboundSqsMessageSupport.detectMessageTypeByScan(rawBody);
        }

        try {
            log.info("Received SQS message queueUrl={}, messageId={}, messageType={}, businessMessageId={}, rawBody={}",
                    queueUrl, message.messageId(), metadata.messageType(), metadata.businessMessageId(), rawBody);
            if (inboundSqsMessageSupport.isSnsNotificationWrapper(rawBody)) {
                log.info("Detected SNS Notification wrapper messageId={}, extractedXmlLength={}",
                        message.messageId(), payload != null ? payload.length() : 0);
            } else {
                log.info("Detected raw XML payload messageId={}, xmlLength={}",
                        message.messageId(), payload != null ? payload.length() : 0);
            }

            if (inboundSqsMessageSupport.isPacs002(metadata.messageType())) {
                pacs002ProcessingService.process(payload);
                log.info("SQS message processed queueUrl={}, messageId={}, messageType={}, processingResult=PROCESSED",
                        queueUrl, message.messageId(), metadata.messageType());
            } else if (inboundSqsMessageSupport.isAdmi002(metadata.messageType())) {
                admi002ProcessingService.process(payload);
                log.info("SQS message processed queueUrl={}, messageId={}, messageType={}, processingResult=PROCESSED_BY_ADMI002_ROUTE",
                        queueUrl, message.messageId(), metadata.messageType());
            } else {
                log.warn("Unsupported inbound message queueUrl={}, messageId={}, messageType={}, processingResult=UNSUPPORTED",
                        queueUrl, message.messageId(), metadata.messageType());
                return;
            }

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
            log.info("Deleted inbound SQS message queueUrl={}, messageId={}, messageType={}",
                    queueUrl, message.messageId(), metadata.messageType());
        } catch (Exception ex) {
            log.error("Failed to process inbound SQS message queueUrl={}, messageId={}, messageType={}, rawBody={}, extractedPayload={}",
                    queueUrl, message.messageId(), metadata.messageType(), rawBody, payload, ex);
        }
    }

    private boolean isLoaded(String value) {
        return value != null && !value.isBlank();
    }
}
