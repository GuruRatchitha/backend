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

    private PartyDetails senderDetails;

    private PartyDetails receiverDetails;

    private PaymentDetails paymentDetails;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartyDetails {
        private String name;
        private String accountNumber;
        private String routingNumber;
        private String bankName;
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
}
