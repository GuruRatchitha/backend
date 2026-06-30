package com.bank.fedwire.service;

import com.bank.fedwire.config.AwsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Admi002ListenerServiceTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private AwsProperties awsProperties;

    @Mock
    private Admi002ProcessingService admi002ProcessingService;

    @Mock
    private InboundSqsMessageSupport inboundSqsMessageSupport;

    @InjectMocks
    private Admi002ListenerService admi002ListenerService;

    @Test
    void pollQueueRoutesAdmi002MessagesToAdmiProcessor() {
        String queueUrl = "https://sqs.us-east-2.amazonaws.com/123456789012/admi002";
        String xml = "<Envelope><AppHdr><MsgDefIdr>admi.002.001.01</MsgDefIdr><BizMsgIdr>MSG-9</BizMsgIdr></AppHdr>"
                + "<Document><RltdRef><Ref>TRF-123</Ref></RltdRef></Document></Envelope>";

        when(awsProperties.getAdmi002QueueUrl()).thenReturn(queueUrl);
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder()
                        .messageId("msg-9")
                        .receiptHandle("receipt-9")
                        .body(xml)
                        .build())
                .build());
        when(inboundSqsMessageSupport.extractXmlPayload(xml)).thenReturn(xml);
        when(inboundSqsMessageSupport.extractMetadata(xml)).thenReturn(
                new InboundSqsMessageSupport.InboundMessageMetadata("admi.002.001.01", "MSG-9"));
        when(inboundSqsMessageSupport.isAdmi002("admi.002.001.01")).thenReturn(true);

        admi002ListenerService.pollQueue();

        verify(admi002ProcessingService).process(xml);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }
}
