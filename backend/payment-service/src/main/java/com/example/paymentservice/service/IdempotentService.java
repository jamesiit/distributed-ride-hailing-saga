package com.example.paymentservice.service;

import com.example.paymentservice.model.IdempotentKey;
import com.example.paymentservice.model.IdempotentKeyStatus;
import com.example.paymentservice.repo.IdempotencyRepo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
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

            Optional<IdempotentKey> optionalKey = idempotencyRepo.findById(convId);

            if (optionalKey.isEmpty()) {
                return IdempotentKeyStatus.PROCESSING;
            }

            IdempotentKey existingKey = optionalKey.get();

            if (existingKey.getIdempotentKeyStatus() == IdempotentKeyStatus.CACHED) {
                return IdempotentKeyStatus.CACHED;
            } else {
                return IdempotentKeyStatus.PROCESSING;
            }

        }

    }

    public IdempotentKey makeCache(String recIdempotentKey) {
        UUID checkId = UUID.fromString(recIdempotentKey);
        return idempotencyRepo.findById(checkId).orElse(null);
    }

    public void updateCreatedStatus(UUID convKey) {

        Optional<IdempotentKey> checkedKey = idempotencyRepo.findById(convKey);

        if (checkedKey.isEmpty()) {
            System.out.println("its empty");
        }

        if (checkedKey.isPresent()) {
            checkedKey.get().setIdempotentKeyStatus(IdempotentKeyStatus.SUCCESS);
            idempotencyRepo.save(checkedKey.get());
        }

    }
}
