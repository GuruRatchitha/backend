package com.bank.fedwire.service;

import com.bank.fedwire.config.AwsProperties;
import com.bank.fedwire.entity.PACS008;
import com.bank.fedwire.repository.PACS008Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnsPublisherServiceTest {

    @Mock
    private PACS008Repository pacs008Repository;

    @Mock
    private SnsClient snsClient;

    @Test
    void publishIfNeededPublishesWrappedXmlToFifoTopic() {
        String topicArn = "arn:aws:sns:us-east-2:084845891305:fan_out_010099_to_fedwire.fifo";
        String xmlPayload = "<FedwireMessage><MessageId>ABC123</MessageId></FedwireMessage>";
        PACS008 pacs008 = PACS008.builder()
                .transactionId(100L)
                .xmlPayload(xmlPayload)
                .build();

        AwsProperties awsProperties = new AwsProperties();
        awsProperties.setTopicArn(topicArn);

        when(pacs008Repository.findByTransactionIdForUpdate(100L)).thenReturn(Optional.of(pacs008));
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder()
                        .messageId("sns-message-id")
                        .build());

        SnsPublisherService service = new SnsPublisherService(pacs008Repository, awsProperties, snsClient);

        service.publishIfNeeded(100L);

        ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(requestCaptor.capture());

        PublishRequest request = requestCaptor.getValue();
        assertEquals(topicArn, request.topicArn());
        assertEquals("{\"Type\":\"Notification\",\"Message\":\"" + xmlPayload.replace("\"", "\\\"")
                + "\",\"TopicArn\":\"" + topicArn + "\"}", request.message());
        assertNotNull(request.messageGroupId());
        assertFalse(request.messageGroupId().isBlank());
        assertNotNull(request.messageDeduplicationId());
        assertFalse(request.messageDeduplicationId().isBlank());
        assertEquals("sns-message-id", pacs008.getSqsMessageId());
        assertNotNull(pacs008.getSqsPublishedAt());
        verify(pacs008Repository).save(pacs008);
    }
}
