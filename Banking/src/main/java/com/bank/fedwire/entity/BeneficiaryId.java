package com.bank.fedwire.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BeneficiaryId implements Serializable {

    private Long userId;

    private String accountNumber;

    private String routingNumber;
}
