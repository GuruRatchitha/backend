package com.bank.fedwire.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        StringBuilder accountNumber = new StringBuilder("2");
        for (int i = 0; i < 10; i++) {
            accountNumber.append(RANDOM.nextInt(10));
        }
        return accountNumber.toString();
    }
}
