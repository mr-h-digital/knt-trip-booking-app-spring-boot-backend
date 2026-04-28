package com.kntransport.backend.service;

import com.kntransport.backend.dto.DriverLocationDto;
import com.kntransport.backend.dto.UpdateLocationRequest;
import com.kntransport.backend.entity.DriverLocation;
import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.BadRequestException;
import com.kntransport.backend.exception.ForbiddenException;
import com.kntransport.backend.exception.ResourceNotFoundException;
import com.kntransport.backend.repository.DriverLocationRepository;
import com.kntransport.backend.repository.TripBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DriverLocationService {

    private final TripBookingRepository  tripRepository;
    private final DriverLocationRepository locationRepository;
    private final UserService            userService;

    public DriverLocationService(TripBookingRepository tripRepository,
                                 DriverLocationRepository locationRepository,
                                 UserService userService) {
        this.tripRepository     = tripRepository;
        this.locationRepository = locationRepository;
        this.userService        = userService;
    }

    @Transactional
    public void updateLocation(String driverEmail, String tripId, UpdateLocationRequest req) {
        User driver = userService.getByEmail(driverEmail);
        TripBooking trip = findTrip(tripId);

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ForbiddenException("You are not assigned to this trip");
        }
        if (trip.getStatus() != TripBooking.TripStatus.IN_PROGRESS) {
            throw new BadRequestException("Location can only be updated for IN_PROGRESS trips");
        }

        DriverLocation loc = locationRepository.findByTrip(trip)
                .orElseGet(() -> {
                    DriverLocation newLoc = new DriverLocation();
                    newLoc.setTrip(trip);
                    return newLoc;
                });

        loc.setLatitude(req.latitude());
        loc.setLongitude(req.longitude());
        loc.setUpdatedAt(LocalDateTime.now());
        locationRepository.save(loc);
    }

    public DriverLocationDto getLocation(String requesterEmail, String tripId) {
        User requester = userService.getByEmail(requesterEmail);
        TripBooking trip = findTrip(tripId);

        if (requester.getRole() != User.Role.ADMIN) {
            if (!trip.getCommuter().getId().equals(requester.getId())) {
                throw new ForbiddenException("You do not have access to this trip's location");
            }
        }

        DriverLocation loc = locationRepository.findByTrip(trip)
                .orElseThrow(() -> new ResourceNotFoundException("No location recorded yet for trip: " + tripId));

        return DriverLocationDto.from(loc);
    }

    private TripBooking findTrip(String id) {
        return tripRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + id));
    }
}
