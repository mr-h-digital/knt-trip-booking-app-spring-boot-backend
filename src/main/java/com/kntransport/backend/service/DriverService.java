package com.kntransport.backend.service;

import com.kntransport.backend.dto.DriverEarningsDto;
import com.kntransport.backend.dto.PagedResponse;
import com.kntransport.backend.dto.TripBookingDto;
import com.kntransport.backend.dto.UpdateTripStatusRequest;
import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.BadRequestException;
import com.kntransport.backend.exception.ResourceNotFoundException;
import com.kntransport.backend.repository.TripBookingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DriverService {

    private final TripBookingRepository tripRepository;
    private final UserService           userService;

    public DriverService(TripBookingRepository tripRepository, UserService userService) {
        this.tripRepository = tripRepository;
        this.userService    = userService;
    }

    /** All trips assigned to this driver, newest first. */
    public PagedResponse<TripBookingDto> getMyTrips(String email, int page, int size) {
        User driver = getDriver(email);
        return PagedResponse.from(
                tripRepository.findByDriverOrderByDateDescTimeDesc(driver, PageRequest.of(page, size)),
                TripBookingDto::from);
    }

    /** Single trip — must belong to this driver. */
    public TripBookingDto getMyTrip(String email, String tripId) {
        User driver = getDriver(email);
        TripBooking trip = findTrip(tripId);
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Trip not found");
        }
        return TripBookingDto.from(trip);
    }

    /**
     * Driver advances the trip status.
     * Allowed transitions:
     *   CONFIRMED / QUOTE_ACCEPTED → IN_PROGRESS
     *   IN_PROGRESS               → COMPLETED
     *   CONFIRMED / IN_PROGRESS   → CANCELLED
     */
    @Transactional
    public TripBookingDto updateStatus(String email, String tripId, UpdateTripStatusRequest req) {
        User driver = getDriver(email);
        TripBooking trip = findTrip(tripId);

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Trip not found");
        }

        TripBooking.TripStatus newStatus = TripBooking.TripStatus.valueOf(req.status());
        TripBooking.TripStatus current   = trip.getStatus();

        boolean valid = switch (newStatus) {
            case IN_PROGRESS -> current == TripBooking.TripStatus.CONFIRMED
                             || current == TripBooking.TripStatus.QUOTE_ACCEPTED;
            case COMPLETED   -> current == TripBooking.TripStatus.IN_PROGRESS;
            case CANCELLED   -> current == TripBooking.TripStatus.CONFIRMED
                             || current == TripBooking.TripStatus.QUOTE_ACCEPTED
                             || current == TripBooking.TripStatus.IN_PROGRESS;
            default          -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                "Cannot transition from " + current + " to " + newStatus);
        }

        trip.setStatus(newStatus);
        return TripBookingDto.from(tripRepository.save(trip));
    }

    /** Summary of driver earnings and trip counts. */
    public DriverEarningsDto getEarnings(String email) {
        User driver = getDriver(email);

        double totalEarnings   = tripRepository.sumCompletedEarningsByDriver(driver);
        long   completedTrips  = tripRepository.countByDriverAndStatus(driver, TripBooking.TripStatus.COMPLETED);
        long   confirmedTrips  = tripRepository.countByDriverAndStatus(driver, TripBooking.TripStatus.CONFIRMED)
                               + tripRepository.countByDriverAndStatus(driver, TripBooking.TripStatus.QUOTE_ACCEPTED);
        long   inProgressTrips = tripRepository.countByDriverAndStatus(driver, TripBooking.TripStatus.IN_PROGRESS);
        double avgEarnings     = completedTrips > 0 ? totalEarnings / completedTrips : 0.0;

        return new DriverEarningsDto(totalEarnings, completedTrips, confirmedTrips, inProgressTrips, avgEarnings);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User getDriver(String email) {
        User user = userService.getByEmail(email);
        if (user.getRole() != User.Role.DRIVER && user.getRole() != User.Role.ADMIN) {
            throw new BadRequestException("Access restricted to driver accounts");
        }
        return user;
    }

    private TripBooking findTrip(String id) {
        return tripRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + id));
    }
}
