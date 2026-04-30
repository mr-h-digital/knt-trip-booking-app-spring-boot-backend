package com.kntransport.backend.service;

import com.kntransport.backend.dto.CancelTripRequest;
import com.kntransport.backend.dto.CreateTripRequest;
import com.kntransport.backend.dto.PagedResponse;
import com.kntransport.backend.dto.QuoteDto;
import com.kntransport.backend.dto.RateTripRequest;
import com.kntransport.backend.dto.TripBookingDto;
import com.kntransport.backend.entity.Quote;
import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.BadRequestException;
import com.kntransport.backend.exception.ResourceNotFoundException;
import com.kntransport.backend.repository.QuoteRepository;
import com.kntransport.backend.repository.TripBookingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TripService {

    private final TripBookingRepository tripRepository;
    private final QuoteRepository       quoteRepository;
    private final UserService           userService;

    public TripService(TripBookingRepository tripRepository,
                       QuoteRepository quoteRepository,
                       UserService userService) {
        this.tripRepository  = tripRepository;
        this.quoteRepository = quoteRepository;
        this.userService     = userService;
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

    public TripBookingDto cancelTrip(String email, String id, CancelTripRequest req) {
        TripBooking trip = findTrip(id);
        if (!trip.getCommuter().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Trip not found");
        }
        if (trip.getStatus() == TripBooking.TripStatus.COMPLETED ||
            trip.getStatus() == TripBooking.TripStatus.CANCELLED) {
            throw new BadRequestException("Trip cannot be cancelled in status: " + trip.getStatus());
        }
        trip.setStatus(TripBooking.TripStatus.CANCELLED);
        return TripBookingDto.from(tripRepository.save(trip));
    }

    public TripBookingDto rateTrip(String email, String id, RateTripRequest req) {
        TripBooking trip = findTrip(id);
        if (!trip.getCommuter().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Trip not found");
        }
        if (trip.getStatus() != TripBooking.TripStatus.COMPLETED) {
            throw new BadRequestException("Only completed trips can be rated");
        }
        trip.setRating(req.rating());
        trip.setRatingComment(req.comment());
        return TripBookingDto.from(tripRepository.save(trip));
    }

    /** Returns all active (non-cancelled) driver quotes for a commuter-owned trip. */
    public List<QuoteDto> getTripQuotes(String email, String tripId) {
        TripBooking trip = findTrip(tripId);
        if (!trip.getCommuter().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Trip not found");
        }
        return quoteRepository
                .findAllByReferenceIdAndReferenceTypeAndCancelledFalse(trip.getId(), Quote.ReferenceType.TRIP)
                .stream()
                .map(QuoteDto::from)
                .collect(Collectors.toList());
    }

    private TripBooking findTrip(String id) {
        return tripRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + id));
    }
}
