package com.bank.fedwire.service;

import com.bank.fedwire.config.AwsProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
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
        String payload = extractXmlPayload(rawBody);
        InboundMessageMetadata metadata = extractMetadata(payload);
        if (metadata.messageType() == null) {
            metadata = extractMetadata(rawBody);
        }
        if (metadata.messageType() == null) {
            metadata = detectMessageTypeByScan(payload);
        }
        if (metadata.messageType() == null) {
            metadata = detectMessageTypeByScan(rawBody);
        }

        try {
            log.info("Received SQS message queueUrl={}, messageId={}, messageType={}, businessMessageId={}, rawBody={}",
                    queueUrl, message.messageId(), metadata.messageType(), metadata.businessMessageId(), rawBody);
            if (isSnsNotificationWrapper(rawBody)) {
                log.info("Detected SNS Notification wrapper messageId={}, extractedXmlLength={}",
                        message.messageId(), payload != null ? payload.length() : 0);
            } else {
                log.info("Detected raw XML payload messageId={}, xmlLength={}",
                        message.messageId(), payload != null ? payload.length() : 0);
            }

            if (isPacs002(metadata.messageType())) {
                pacs002ProcessingService.process(payload);
                log.info("SQS message processed queueUrl={}, messageId={}, messageType={}, processingResult=PROCESSED",
                        queueUrl, message.messageId(), metadata.messageType());
            } else if ("admi.002.001.01".equalsIgnoreCase(metadata.messageType())) {
                admi002ProcessingService.process(payload);
                log.info("SQS message processed queueUrl={}, messageId={}, messageType={}, processingResult=PROCESSED",
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

    private String extractXmlPayload(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }

        String trimmed = body.trim();
        if (!trimmed.startsWith("{")) {
            return body;
        }

        String message = extractJsonStringField(trimmed, "Message");
        if (message != null && !message.isBlank()) {
            return message;
        }

        return body;
    }

    private boolean isSnsNotificationWrapper(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        return body.trim().startsWith("{");
    }

    private boolean isPacs002(String messageType) {
        return messageType != null && messageType.toLowerCase().startsWith("pacs.002.");
    }

    private String extractJsonStringField(String json, String fieldName) {
        String needle = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(needle);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + needle.length());
        if (colonIndex < 0) {
            return null;
        }

        int startQuote = json.indexOf('"', colonIndex + 1);
        if (startQuote < 0) {
            return null;
        }

        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaping) {
                switch (ch) {
                    case '"', '\\', '/' -> value.append(ch);
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> {
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                value.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ex) {
                                log.warn("Failed to decode JSON unicode escape for fieldName={} at index={} json={}",
                                        fieldName, i, json, ex);
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }
                    default -> value.append(ch);
                }
                escaping = false;
                continue;
            }

            if (ch == '\\') {
                escaping = true;
                continue;
            }

            if (ch == '"') {
                return value.toString();
            }

            value.append(ch);
        }

        return null;
    }

    private boolean isLoaded(String value) {
        return value != null && !value.isBlank();
    }

    private InboundMessageMetadata extractMetadata(String xmlPayload) {
        if (xmlPayload == null || xmlPayload.isBlank()) {
            return new InboundMessageMetadata(null, null);
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            factory.setNamespaceAware(true);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlPayload)));
            document.getDocumentElement().normalize();

            return new InboundMessageMetadata(
                    findFirstText(document, "MsgDefIdr"),
                    findFirstText(document, "BizMsgIdr"));
        } catch (Exception ex) {
            log.warn("Unable to extract inbound metadata from XML payload length={}",
                    xmlPayload.length(), ex);
            return new InboundMessageMetadata(null, null);
        }
    }

    private InboundMessageMetadata detectMessageTypeByScan(String body) {
        if (body == null || body.isBlank()) {
            return new InboundMessageMetadata(null, null);
        }

        String normalized = body.toLowerCase();
        String messageType = null;
        if (normalized.contains("admi.002.001.01")) {
            messageType = "admi.002.001.01";
        } else if (normalized.contains("pacs.002.001.10")) {
            messageType = "pacs.002.001.10";
        }

        return new InboundMessageMetadata(messageType, findFirstTextFromBody(body, "BizMsgIdr"));
    }

    private String findFirstTextFromBody(String xmlPayload, String localName) {
        if (xmlPayload == null || xmlPayload.isBlank()) {
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            factory.setNamespaceAware(true);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlPayload)));
            document.getDocumentElement().normalize();
            return findFirstText(document, localName);
        } catch (Exception ex) {
            return null;
        }
    }

    private String findFirstText(Document document, String... localNames) {
        for (String localName : localNames) {
            NodeList nodes = document.getElementsByTagNameNS("*", localName);
            if (nodes == null || nodes.getLength() == 0) {
                nodes = document.getElementsByTagName(localName);
            }

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element element) {
                    String text = element.getTextContent();
                    if (text != null && !text.isBlank()) {
                        return text.trim();
                    }
                }
            }
        }
        return null;
    }

    private record InboundMessageMetadata(String messageType, String businessMessageId) {
    }
}
