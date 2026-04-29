package com.example.paymentservice.repo;

import com.example.paymentservice.model.IdempotentKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IdempotencyRepo extends JpaRepository<IdempotentKey, UUID> {
}
