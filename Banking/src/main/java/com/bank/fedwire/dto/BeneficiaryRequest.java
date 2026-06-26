package com.bank.fedwire.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeneficiaryRequest {

    private String beneficiaryName;

    private String townName;

    private String accountNumber;

//    private String confirmAccountNumber;
    private String routingNumber;

    private String bankName;

    private String status;
}
