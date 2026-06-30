package com.bank.fedwire.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RoutingNumberGenerator {

    private static final int MIN_ROUTING_NUMBER = 100_000_000;
    private static final int ROUTING_NUMBER_BOUND = 900_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return String.valueOf(MIN_ROUTING_NUMBER + secureRandom.nextInt(ROUTING_NUMBER_BOUND));
    }
}
