package com.example.paymentservice.service;

import com.example.paymentservice.model.IdempotentKey;
import com.example.paymentservice.model.IdempotentKeyStatus;
import com.example.paymentservice.repo.IdempotencyRepo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class IdempotentService {

    private IdempotencyRepo idempotencyRepo;

    public IdempotentService(IdempotencyRepo idempotencyRepo) {
        this.idempotencyRepo = idempotencyRepo;
    }

    public IdempotentKeyStatus processRequest(String recIdempotentKey) {

        UUID convId = UUID.fromString(recIdempotentKey);

        try {

            idempotencyRepo.insert(convId, "PENDING");

            return IdempotentKeyStatus.PROCEED;

        } catch (DataIntegrityViolationException e) {

            IdempotentKey key = idempotencyRepo.findById(convId).orElse(null);

            if (key.getIdempotentKeyStatus().equals("SUCCESS")) {

                return IdempotentKeyStatus.CACHED;

            }

            else {
                return IdempotentKeyStatus.PROCESSING;
            }
        }

    }
}
