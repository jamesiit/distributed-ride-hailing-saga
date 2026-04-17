package com.example.dispatchservice.dto;

import java.util.UUID;

public class DispatchDTO {

    private UUID tripId;

    private String cabNo;

    private String cabDriver;

    public DispatchDTO(UUID tripId, String cabNo, String cabDriver) {
        this.tripId = tripId;
        this.cabNo = cabNo;
        this.cabDriver = cabDriver;
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
}
