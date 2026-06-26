package com.bank.fedwire.util;

import org.springframework.stereotype.Component;

@Component
public class IbanGenerator {

    public String generate(String accountNumber, Long accountId) {
        return "US" + accountNumber + String.format("%08d", accountId);
    }
}
