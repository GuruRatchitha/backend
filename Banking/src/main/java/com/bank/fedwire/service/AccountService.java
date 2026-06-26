package com.bank.fedwire.service;

import com.bank.fedwire.dto.AccountRequest;
import com.bank.fedwire.dto.AccountResponse;

public interface AccountService {

    AccountResponse createAccount(AccountRequest request);
}
