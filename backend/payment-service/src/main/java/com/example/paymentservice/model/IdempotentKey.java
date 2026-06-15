package com.example.paymentservice.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "idempotent_key_table")
public class IdempotentKey {

    @Id
    @Column(name = "idempotent_key")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID idempotentKey;

    @Enumerated(EnumType.STRING)
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
