package com.example.paymentservice.repo;

import com.example.paymentservice.model.IdempotentKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IdempotencyRepo extends JpaRepository<IdempotentKey, UUID> {

    @Modifying
    @Query(value = "insert into idempotent_key_table (idempotentKey, idempotentKeyStatus) values (:id, :status)", nativeQuery = true)
    void insert(@Param("id") UUID recIdempotentKey, @Param("status") String pending);
}
