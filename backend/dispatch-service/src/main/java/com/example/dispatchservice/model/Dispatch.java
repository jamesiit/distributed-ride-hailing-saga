package com.example.dispatchservice.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "dispatch_table")
public class Dispatch {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "dispatch_id")
    private UUID dispatchId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "cab_no")
    private String cabNo;

    @Column(name = "cab_driver")
    private String cabDriver;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status")
    private DispatchStatus dispatchStatus;

    @Column(name="pickup_location")
    private String pickupLocation;

    public Dispatch(UUID dispatchId, UUID tripId, String cabNo, String cabDriver, DispatchStatus dispatchStatus, String pickupLocation) {
        this.dispatchId = dispatchId;
        this.tripId = tripId;
        this.cabNo = cabNo;
        this.cabDriver = cabDriver;
        this.dispatchStatus = dispatchStatus;
        this.pickupLocation = pickupLocation;
    }

    public UUID getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(UUID dispatchId) {
        this.dispatchId = dispatchId;
    }

    public UUID getTripId() {
        return tripId;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }

    public String getCabNo() {
        return cabNo;
    }

    public void setCabNo(String cabNo) {
        this.cabNo = cabNo;
    }

    public String getCabDriver() {
        return cabDriver;
    }

    public void setCabDriver(String cabDriver) {
        this.cabDriver = cabDriver;
    }

    public DispatchStatus getDispatchStatus() {
        return dispatchStatus;
    }

    public void setDispatchStatus(DispatchStatus dispatchStatus) {
        this.dispatchStatus = dispatchStatus;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public Dispatch() {
    }

}
