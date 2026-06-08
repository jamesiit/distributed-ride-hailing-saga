package com.example.paymentservice.repo;

import com.example.paymentservice.model.IdempotentKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface IdempotencyRepo extends JpaRepository<IdempotentKey, UUID> {

    @Modifying
    @Transactional
    @Query(value = "insert into idempotent_key_table (idempotent_key, idempotent_key_status) values (:id, :status)", nativeQuery = true)
    void insert(@Param("id") String recIdempotentKey, @Param("status") String pending);
}
