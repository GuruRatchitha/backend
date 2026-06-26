package com.bank.fedwire.exception;

public class DuplicateAccountNumberException extends DuplicateResourceException {

    public DuplicateAccountNumberException(String message) {
        super(message);
    }
}
