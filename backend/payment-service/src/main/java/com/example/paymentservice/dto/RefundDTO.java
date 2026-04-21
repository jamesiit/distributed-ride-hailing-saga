package com.example.paymentservice.dto;

import java.util.UUID;

public class RefundDTO {

    private final UUID tripId;

    public RefundDTO(UUID tripId) {
        this.tripId = tripId;
    }

    public UUID getTripId() {
        return tripId;
    }
}
