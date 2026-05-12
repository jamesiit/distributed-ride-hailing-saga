package com.example.tripservice.service;

import com.example.tripservice.dto.TripDTO;
import com.example.tripservice.model.Trip;
import com.example.tripservice.model.TripStatus;
import com.example.tripservice.repo.TripRepo;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class TripService {

    private TripRepo tripRepo;

    public TripService(TripRepo tripRepo) {
        this.tripRepo = tripRepo;
    }

    public Trip createTrip(TripDTO tripDTO) {
            Trip createTrip = new Trip();

            createTrip.setContactNumber(tripDTO.getContactNumber());
            createTrip.setDropOffLocation(tripDTO.getDropOffLocation());
            createTrip.setPickUpLocation(tripDTO.getPickUpLocation());
            createTrip.setTripStatus(TripStatus.PENDING);

            tripRepo.save(createTrip);

            return createTrip;

    }


    public Trip processRefund(UUID tripId) {

        Trip checkTrip = tripRepo.findById(tripId).orElse(null);

        if (checkTrip == null) {
            return null;
        }

        checkTrip.setTripStatus(TripStatus.FAILED);
        tripRepo.save(checkTrip);

        return checkTrip;

    }

    public Trip completeTrip(UUID tripId) {

        Trip checkTrip = tripRepo.findById(tripId).orElse(null);

        if (checkTrip == null) {
            return null;
        }

        checkTrip.setTripStatus(TripStatus.COMPLETED);
        tripRepo.save(checkTrip);

        return checkTrip;

    }
}
