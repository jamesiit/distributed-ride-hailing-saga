package com.example.dispatchservice.dto;

import java.util.UUID;

public class DispatchDTO {

    private UUID tripId;

    private String cabNo;

    private String cabDriver;

    private String pickupLocation;

    public DispatchDTO(UUID tripId, String cabNo, String cabDriver, String pickupLocation) {
        this.tripId = tripId;
        this.cabNo = cabNo;
        this.cabDriver = cabDriver;
        this.pickupLocation = pickupLocation;
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

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }
}
