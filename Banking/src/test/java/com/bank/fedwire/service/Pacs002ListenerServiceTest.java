package com.bank.fedwire.service;

import com.bank.fedwire.config.AwsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Pacs002ListenerServiceTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private AwsProperties awsProperties;

    @Mock
    private Pacs002ProcessingService pacs002ProcessingService;

    @Mock
    private Admi002ProcessingService admi002ProcessingService;

    @InjectMocks
    private Pacs002ListenerService pacs002ListenerService;

    @Test
    void pollQueueExtractsXmlFromSnsNotificationWrapper() {
        String queueUrl = "https://sqs.us-east-2.amazonaws.com/123456789012/pacs002";
        String xml = "<Envelope><AppHdr><MsgDefIdr>pacs.002.001.10</MsgDefIdr><BizMsgIdr>MSG-2</BizMsgIdr></AppHdr>"
                + "<Document><transaction_status>ACCP</transaction_status></Document></Envelope>";
        String snsWrapper = "{\"Type\":\"Notification\",\"Message\":\"" + xml.replace("\"", "\\\"") + "\"}";

        when(awsProperties.getPacs002QueueUrl()).thenReturn(queueUrl);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder()
                        .messageId("msg-1")
                        .receiptHandle("receipt-1")
                        .body(snsWrapper)
                        .build())
                .build());

        pacs002ListenerService.pollQueue();

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(pacs002ProcessingService).process(payloadCaptor.capture());
        assertEquals(xml, payloadCaptor.getValue());
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void pollQueueRoutesAdmi002MessagesToAdmiProcessor() {
        String queueUrl = "https://sqs.us-east-2.amazonaws.com/123456789012/pacs002";
        String xml = "<Envelope><AppHdr><MsgDefIdr>admi.002.001.01</MsgDefIdr><BizMsgIdr>MSG-9</BizMsgIdr></AppHdr>"
                + "<Document><RltdRef><Ref>TRF-123</Ref></RltdRef></Document></Envelope>";

        when(awsProperties.getPacs002QueueUrl()).thenReturn(queueUrl);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder()
                        .messageId("msg-9")
                        .receiptHandle("receipt-9")
                        .body(xml)
                        .build())
                .build());

        pacs002ListenerService.pollQueue();

        verify(admi002ProcessingService).process(xml);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }
}
