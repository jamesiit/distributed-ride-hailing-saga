package com.example.paymentservice.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "payment_table")
public class Payment {

    @Id
    @UuidGenerator( style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "trip_id")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID tripId;

    @Column(name = "payment_amount")
    private Long paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    public Payment(UUID paymentId, UUID tripId, Long paymentAmount, PaymentStatus paymentStatus) {
        this.paymentId = paymentId;
        this.tripId = tripId;
        this.paymentAmount = paymentAmount;
        this.paymentStatus = paymentStatus;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getTripId() {
        return tripId;
    }

    public Long getPaymentAmount() {
        return paymentAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentAmount(Long paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public Payment() {
    }
}
