package com.example.paymentservice.service;

import com.example.paymentservice.model.IdempotentKeyStatus;
import org.springframework.stereotype.Service;

@Service
public class IdempotentService {
    public IdempotentKeyStatus processRequest(String recIdempotentKey) {

        return IdempotentKeyStatus.PENDING;

    }
}
