package com.kntransport.backend.repository;

import com.kntransport.backend.entity.DriverLocation;
import com.kntransport.backend.entity.TripBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, UUID> {
    Optional<DriverLocation> findByTrip(TripBooking trip);
}
