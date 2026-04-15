package com.example.tripservice.service;

import com.example.tripservice.dto.TripDTO;
import com.example.tripservice.model.Trip;
import com.example.tripservice.model.TripStatus;
import com.example.tripservice.repo.TripRepo;
import org.springframework.stereotype.Service;

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
}
