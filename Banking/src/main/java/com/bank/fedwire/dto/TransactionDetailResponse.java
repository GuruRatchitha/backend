package com.bank.fedwire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetailResponse {

    private Long transactionId;

    private String status;

    private String transactionStatus;

    private String payaptStatus;

    private String rejectionReason;

    private String pacs002Reason;

    private String admi002Reason;

    private LocalDateTime paymentTimestamp;

    private String uetr;

    private Boolean hasPacs008;

    private Boolean hasPacs002;

    private Boolean hasAdmi002;

    private String pacs008Xml;

    private String pacs002Xml;

    private String admi002Xml;

    private Boolean canRevert;

    private Boolean reverted;

    private Boolean settlementCompleted;

    private Boolean processCompleted;

    private String currentPipelineStep;

    private SenderDetails senderDetails;

    private ReceiverDetails receiverDetails;

    private PaymentDetails paymentDetails;

    private XmlMessages xmlMessages;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SenderDetails {
        private String senderName;
        private String senderAccountNumber;
        private String senderRoutingNumber;
        private String senderBankName;
        private String senderCountry;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReceiverDetails {
        private String receiverName;
        private String receiverAccountNumber;
        private String receiverRoutingNumber;
        private String receiverBankName;
        private String receiverCountry;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentDetails {
        private String transactionReference;
        private BigDecimal amount;
        private LocalDateTime paymentDate;
        private String status;
        private String channel;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class XmlMessages {
        private String pacs008;
        private String pacs002;
        private String admi002;
    }
}
