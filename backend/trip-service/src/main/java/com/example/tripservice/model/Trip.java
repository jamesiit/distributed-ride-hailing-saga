package com.example.tripservice.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "trip_table")
public class Trip {

    @Id
    @Column(name = "trip_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID tripId;

    @Column(name= "contact_number")
    private String contactNumber;

    @Column(name = "pickup_location")
    private String pickUpLocation;

    @Column(name = "drop_off_location")
    private String dropOffLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_status")
    private TripStatus tripStatus;

    public Trip(UUID tripId, String contactNumber, String pickUpLocation, String dropOffLocation, TripStatus tripStatus) {
        this.tripId = tripId;
        this.contactNumber = contactNumber;
        this.pickUpLocation = pickUpLocation;
        this.dropOffLocation = dropOffLocation;
        this.tripStatus = tripStatus;
    }

    public UUID getTripId() {
        return tripId;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getPickUpLocation() {
        return pickUpLocation;
    }

    public String getDropOffLocation() {
        return dropOffLocation;
    }

    public TripStatus getTripStatus() {
        return tripStatus;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setPickUpLocation(String pickUpLocation) {
        this.pickUpLocation = pickUpLocation;
    }

    public void setDropOffLocation(String dropOffLocation) {
        this.dropOffLocation = dropOffLocation;
    }

    public void setTripStatus(TripStatus tripStatus) {
        this.tripStatus = tripStatus;
    }

    public Trip() {
    }
}
