package com.example.tripservice.repo;

import com.example.tripservice.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TripRepo extends JpaRepository<Trip, UUID> {
}
