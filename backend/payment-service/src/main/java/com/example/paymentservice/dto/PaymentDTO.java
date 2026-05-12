package com.example.paymentservice.dto;

import java.util.UUID;

public class PaymentDTO {

    private UUID tripId;

    private Long paymentAmount;

    public PaymentDTO(Long paymentAmount, UUID tripId) {
        this.paymentAmount = paymentAmount;
        this.tripId = tripId;
    }

    public UUID getTripId() {
        return tripId;
    }

    public Long getPaymentAmount() {
        return paymentAmount;
    }
}
