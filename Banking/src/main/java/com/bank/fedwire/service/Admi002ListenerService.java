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
public class Admi002ListenerService {

    private static final AtomicLong POLL_INVOCATION_COUNT = new AtomicLong(0L);

    private final SqsClient sqsClient;
    private final AwsProperties awsProperties;
    private final Admi002ProcessingService admi002ProcessingService;
    private final InboundSqsMessageSupport inboundSqsMessageSupport;

    @PostConstruct
    public void logStartup() {
        log.info("ADMI002 listener initialized queueUrl={}, region={}, accessKeyLoaded={}, secretKeyLoaded={}",
                awsProperties.getAdmi002QueueUrl(),
                awsProperties.getRegion(),
                isLoaded(awsProperties.getAccessKey()),
                isLoaded(awsProperties.getSecretKey()));
    }

    @Scheduled(fixedDelayString = "${aws.admi002-poll-delay-ms:5000}")
    public void pollQueue() {
        long invocation = POLL_INVOCATION_COUNT.incrementAndGet();
        String queueUrl = awsProperties.getAdmi002QueueUrl();
        log.info("ADMI002 pollQueue invoked invocation={}, timestamp={}, queueUrl={}, region={}",
                invocation, Instant.now(), queueUrl, awsProperties.getRegion());
        if (queueUrl == null || queueUrl.isBlank()) {
            log.warn("ADMI002 queue URL is not configured, skipping poll");
            return;
        }

        try {
            log.info("Calling receiveMessage for ADMI002 invocation={}, queueUrl={}, maxMessages={}, waitTimeSeconds={}",
                    invocation, queueUrl, 10, 20);
            ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(20)
                    .build());

            log.info("ADMI002 receiveMessage completed invocation={}, queueUrl={}, messageCount={}",
                    invocation, queueUrl, response.messages().size());
            for (Message message : response.messages()) {
                log.info("ADMI002 SQS message received invocation={}, sqsMessageId={}, bodyLength={}",
                        invocation, message.messageId(), message.body() != null ? message.body().length() : 0);
                handleMessage(queueUrl, message);
            }
        } catch (Exception ex) {
            log.error("Failed to poll ADMI002 queue {}", queueUrl, ex);
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
            log.info("Received ADMI002 SQS message queueUrl={}, sqsMessageId={}, messageType={}, businessMessageId={}, rawBody={}",
                    queueUrl, message.messageId(), metadata.messageType(), metadata.businessMessageId(), rawBody);
            if (!inboundSqsMessageSupport.isAdmi002(metadata.messageType())) {
                log.warn("Unsupported ADMI002 queue message queueUrl={}, sqsMessageId={}, messageType={}, processingResult=UNSUPPORTED",
                        queueUrl, message.messageId(), metadata.messageType());
                return;
            }

            admi002ProcessingService.process(payload);
            log.info("ADMI002 processing completed queueUrl={}, sqsMessageId={}, messageType={}",
                    queueUrl, message.messageId(), metadata.messageType());

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
            log.info("Deleted ADMI002 SQS message queueUrl={}, sqsMessageId={}, messageType={}",
                    queueUrl, message.messageId(), metadata.messageType());
        } catch (Exception ex) {
            log.error("Failed to process ADMI002 SQS message queueUrl={}, sqsMessageId={}, messageType={}, rawBody={}, extractedPayload={}",
                    queueUrl, message.messageId(), metadata.messageType(), rawBody, payload, ex);
        }
    }

    private boolean isLoaded(String value) {
        return value != null && !value.isBlank();
    }
}
