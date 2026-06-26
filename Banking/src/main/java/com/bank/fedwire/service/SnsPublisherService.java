package com.bank.fedwire.service;

import com.bank.fedwire.config.AwsProperties;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.repository.PACS008Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(prefix = "aws", name = "messaging-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SnsPublisherService {

    private static final String MESSAGE_TYPE = "Notification";
    private static final char[] ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final PACS008Repository pacs008Repository;
    private final AwsProperties awsProperties;
    private final SnsClient snsClient;

    @Transactional
    public void publishIfNeeded(Long transactionId) {
        PACS008 pacs008 = pacs008Repository.findByTransactionIdForUpdate(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "PACS008 record not found for transactionId " + transactionId));

        if (pacs008.getXmlPayload() == null || pacs008.getXmlPayload().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "PACS008 XML payload is not available for transactionId " + transactionId);
        }

        if (pacs008.getSqsPublishedAt() != null) {
            log.info("Skipping SNS publish for transactionId={} because it was already sent as messageId={}",
                    transactionId, pacs008.getSqsMessageId());
            return;
        }

        try {
            PublishRequest.Builder builder = PublishRequest.builder()
                    .topicArn(awsProperties.getTopicArn())
                    .message(buildMessageBody(pacs008.getXmlPayload()));

            if (isFifoTopic()) {
                builder.messageGroupId(randomAlphanumeric(12));
                builder.messageDeduplicationId(UUID.randomUUID().toString().replace("-", ""));
            }

            PublishResponse response = snsClient.publish(builder.build());

            pacs008.setSqsPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
            pacs008.setSqsMessageId(response.messageId());
            pacs008Repository.save(pacs008);

            log.info("Published PACS008 to SNS transactionId={}, messageId={}, snsResult={}, topicArn={}",
                    transactionId, response.messageId(), response.sdkHttpResponse().statusCode(), awsProperties.getTopicArn());
        } catch (Exception ex) {
            log.error("SNS publish failed for transactionId={}", transactionId, ex);
            throw ex;
        }
    }

    private boolean isFifoTopic() {
        String topicArn = awsProperties.getTopicArn();
        return topicArn != null && topicArn.endsWith(".fifo");
    }

    private String randomAlphanumeric(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = ThreadLocalRandom.current().nextInt(ALPHANUMERIC.length);
            builder.append(ALPHANUMERIC[index]);
        }
        return builder.toString();
    }

    private String buildMessageBody(String xmlPayload) {
        return "{"
                + "\"Type\":\"" + MESSAGE_TYPE + "\","
                + "\"Message\":\"" + escapeJson(xmlPayload) + "\","
                + "\"TopicArn\":\"" + escapeJson(awsProperties.getTopicArn()) + "\""
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
