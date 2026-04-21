package com.example.tripservice.controller;

import com.example.tripservice.dto.RefundRequestDTO;
import com.example.tripservice.dto.TripDTO;
import com.example.tripservice.model.Trip;
import com.example.tripservice.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/trip")
    public String test() {
        return "Hi";
    }

    @PostMapping("/trip")
    public ResponseEntity<?> createTrip(@RequestBody TripDTO tripDTO) {

        try {
            Trip createTrip = tripService.createTrip(tripDTO);

            return new ResponseEntity<>(createTrip.getTripId(), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Something went wrong!", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("trip/refund")
    public ResponseEntity<?> refundTrip(@RequestBody RefundRequestDTO refundRequestDTO) {

        Trip refundTrip = tripService.processRefund(refundRequestDTO.getTripId());

        if (refundTrip == null) {
            return new ResponseEntity<>("Trip does not exist!", HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(refundTrip, HttpStatus.OK);
    }

}
