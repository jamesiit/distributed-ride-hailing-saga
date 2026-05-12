package com.example.dispatchservice.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "dispatch_table")
public class Dispatch {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "dispatch_id")
    private UUID dispatchId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "cab_no")
    private String cabNo;

    @Column(name = "cab_driver")
    private String cabDriver;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status")
    private DispatchStatus dispatchStatus;

    public Dispatch(UUID dispatchId, UUID tripId, String cabNo, String cabDriver, DispatchStatus dispatchStatus) {
        this.dispatchId = dispatchId;
        this.tripId = tripId;
        this.cabNo = cabNo;
        this.cabDriver = cabDriver;
        this.dispatchStatus = dispatchStatus;
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

    public Dispatch() {
    }

}
