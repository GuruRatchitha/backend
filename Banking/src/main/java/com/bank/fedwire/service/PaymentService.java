package com.bank.fedwire.service;

import com.bank.fedwire.dto.PaymentRequest;
import com.bank.fedwire.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse initiatePayment(Long userId, PaymentRequest request);
}
