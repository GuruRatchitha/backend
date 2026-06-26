package com.bank.fedwire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pacs002MessageDto {

    private String originalMessageId;

    private String messageId;

    private String transferId;

    private String transactionStatus;

    private String reasonCode;

    private String xmlPayload;
}
