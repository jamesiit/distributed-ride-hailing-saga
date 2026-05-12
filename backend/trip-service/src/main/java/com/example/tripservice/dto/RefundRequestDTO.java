package com.example.tripservice.dto;

import java.util.UUID;

public class RefundRequestDTO {
    private final UUID tripId;

    public RefundRequestDTO(UUID tripId) {
        this.tripId = tripId;
    }

    public UUID getTripId() {
        return tripId;

    }
}
