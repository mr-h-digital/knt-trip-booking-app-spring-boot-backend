package com.kntransport.backend.service;

import com.kntransport.backend.dto.CreateTripRequest;
import com.kntransport.backend.dto.PagedResponse;
import com.kntransport.backend.dto.TripBookingDto;
import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.ResourceNotFoundException;
import com.kntransport.backend.repository.TripBookingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
public class TripService {

    private final TripBookingRepository tripRepository;
    private final UserService userService;

    public TripService(TripBookingRepository tripRepository, UserService userService) {
        this.tripRepository = tripRepository;
        this.userService = userService;
    }

    public PagedResponse<TripBookingDto> getMyTrips(String email, int page, int size) {
        User user = userService.getByEmail(email);
        var pageResult = tripRepository.findByCommuterOrderByDateDescTimeDesc(
                user, PageRequest.of(page, size));
        return PagedResponse.from(pageResult, TripBookingDto::from);
    }

    public TripBookingDto getTrip(String email, String id) {
        TripBooking trip = findTrip(id);
        if (!trip.getCommuter().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Trip not found");
        }
        return TripBookingDto.from(trip);
    }

    public TripBookingDto createTrip(String email, CreateTripRequest req) {
        User user = userService.getByEmail(email);

        TripBooking trip = new TripBooking();
        trip.setCommuter(user);
        trip.setPickupAddress(req.pickupAddress());
        trip.setDropAddress(req.dropAddress());
        trip.setDate(LocalDate.parse(req.date()));
        trip.setTime(LocalTime.parse(req.time()));
        trip.setPassengers(req.passengers());
        trip.setNotes(req.notes() != null ? req.notes() : "");
        trip.setStatus(TripBooking.TripStatus.PENDING_QUOTE);

        return TripBookingDto.from(tripRepository.save(trip));
    }

    private TripBooking findTrip(String id) {
        return tripRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + id));
    }
}
