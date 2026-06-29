package com.bank.fedwire.service;

import com.bank.fedwire.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class Pacs002ListenerService {

    private final SqsClient sqsClient;
    private final AwsProperties awsProperties;
    private final Pacs002ProcessingService pacs002ProcessingService;

    @Scheduled(fixedDelayString = "${aws.pacs002-poll-delay-ms:5000}")
    public void pollQueue() {
        String queueUrl = awsProperties.getPacs002QueueUrl();
        if (queueUrl == null || queueUrl.isBlank()) {
            log.warn("PACS002 queue URL is not configured, skipping poll");
            return;
        }

        try {
            ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(20)
                    .build());

            for (Message message : response.messages()) {
                handleMessage(queueUrl, message);
            }
        } catch (Exception ex) {
            log.error("Failed to poll PACS002 queue {}", queueUrl, ex);
        }
    }

    private void handleMessage(String queueUrl, Message message) {
        try {
            pacs002ProcessingService.process(message.body());
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
            log.info("Deleted PACS002 SQS message {} after successful processing", message.messageId());
        } catch (Exception ex) {
            log.error("Failed to process PACS002 SQS message {}", message.messageId(), ex);
        }
    }
}
