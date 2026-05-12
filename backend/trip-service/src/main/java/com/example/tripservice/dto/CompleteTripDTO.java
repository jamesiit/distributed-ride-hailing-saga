package com.example.tripservice.dto;

import java.util.UUID;

public class CompleteTripDTO {

    private final UUID tripId;

    public CompleteTripDTO(UUID tripId) {
        this.tripId = tripId;
    }

    public UUID getTripId() {
        return tripId;

    }
}
