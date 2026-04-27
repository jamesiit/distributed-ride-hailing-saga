package com.example.paymentservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "idempotent_key_table")
public class IdempotentKey {

    @Id
    @Column(name = "idempotent_key")
    private UUID idempotentKey;

    @Column(name = "idempotent_key_status")
    private IdempotentKeyStatus idempotentKeyStatus;

    public IdempotentKey(UUID idempotentKey, IdempotentKeyStatus idempotentKeyStatus) {
        this.idempotentKey = idempotentKey;
        this.idempotentKeyStatus = idempotentKeyStatus;
    }

    public UUID getIdempotentKey() {
        return idempotentKey;
    }

    public void setIdempotentKey(UUID idempotentKey) {
        this.idempotentKey = idempotentKey;
    }

    public IdempotentKeyStatus getIdempotentKeyStatus() {
        return idempotentKeyStatus;
    }

    public void setIdempotentKeyStatus(IdempotentKeyStatus idempotentKeyStatus) {
        this.idempotentKeyStatus = idempotentKeyStatus;
    }

    public IdempotentKey() {
    }

}
