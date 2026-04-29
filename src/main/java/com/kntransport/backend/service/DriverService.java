package com.kntransport.backend.service;

import com.kntransport.backend.dto.*;
import com.kntransport.backend.entity.Quote;
import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.BadRequestException;
import com.kntransport.backend.exception.ResourceNotFoundException;
import com.kntransport.backend.repository.QuoteRepository;
import com.kntransport.backend.repository.TripBookingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DriverService {

    private final TripBookingRepository tripRepository;
    private final QuoteRepository       quoteRepository;
    private final UserService           userService;

    public DriverService(TripBookingRepository tripRepository,
                         QuoteRepository quoteRepository,
                         UserService userService) {
        this.tripRepository  = tripRepository;
        this.quoteRepository = quoteRepository;
        this.userService     = userService;
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

    /** Driver cancels a trip they are assigned to. */
    @Transactional
    public TripBookingDto cancelTrip(String email, String tripId, CancelTripRequest req) {
        User driver = getDriver(email);
        TripBooking trip = findTrip(tripId);

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Trip not found");
        }
        if (trip.getStatus() == TripBooking.TripStatus.COMPLETED ||
            trip.getStatus() == TripBooking.TripStatus.CANCELLED) {
            throw new BadRequestException("Trip cannot be cancelled in status: " + trip.getStatus());
        }
        trip.setStatus(TripBooking.TripStatus.CANCELLED);
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

    // ── Available trips (Option C marketplace) ────────────────────────────────

    /** All PENDING_QUOTE trips visible to any authenticated driver. */
    public PagedResponse<TripBookingDto> browseOpenTrips(int page, int size) {
        return PagedResponse.from(
                tripRepository.findByStatusOrderByDateAscTimeAsc(
                        TripBooking.TripStatus.PENDING_QUOTE, PageRequest.of(page, size)),
                TripBookingDto::from);
    }

    /** Driver submits a quote for a PENDING_QUOTE trip. */
    @Transactional
    public QuoteDto createQuote(String email, String tripId, DriverQuoteRequest req) {
        User driver = getDriver(email);
        TripBooking trip = findTrip(tripId);

        if (trip.getStatus() != TripBooking.TripStatus.PENDING_QUOTE) {
            throw new BadRequestException("Trip is not available for quoting (status: " + trip.getStatus() + ")");
        }

        // One active quote per driver per trip
        quoteRepository.findByReferenceIdAndReferenceTypeAndCreatedByDriverId(
                trip.getId(), Quote.ReferenceType.TRIP, driver.getId())
                .ifPresent(existing -> {
                    if (!existing.isCancelled()) {
                        throw new BadRequestException("You have already submitted a quote for this trip");
                    }
                });

        Quote quote = new Quote();
        quote.setReferenceId(trip.getId());
        quote.setReferenceType(Quote.ReferenceType.TRIP);
        quote.setAmount(req.amount());
        quote.setDriverNote(req.driverNote() != null ? req.driverNote() : "");
        quote.setCreatedByDriver(driver);
        quote.setAccepted(null); // pending commuter response

        // Snapshot driver onto the trip and move to QUOTE_SENT
        trip.setDriver(driver);
        trip.setDriverName(driver.getName());
        if (driver.getCurrentVehicle() != null) {
            trip.setVehicle(driver.getCurrentVehicle());
            trip.setVehicleInfo(driver.getCurrentVehicle().getMake() + " "
                    + driver.getCurrentVehicle().getModel() + " — "
                    + driver.getCurrentVehicle().getColour());
            trip.setVehiclePlate(driver.getCurrentVehicle().getPlate());
        }
        trip.setStatus(TripBooking.TripStatus.QUOTE_SENT);
        trip.setQuotedAmount(req.amount());
        tripRepository.save(trip);

        return QuoteDto.from(quoteRepository.save(quote));
    }

    /** Driver edits their pending quote (only while commuter has not yet responded). */
    @Transactional
    public QuoteDto editQuote(String email, String quoteId, DriverQuoteRequest req) {
        User driver = getDriver(email);
        Quote quote = findQuote(quoteId);

        if (quote.getCreatedByDriver() == null ||
                !quote.getCreatedByDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Quote not found");
        }
        if (Boolean.TRUE.equals(quote.getAccepted()) || Boolean.FALSE.equals(quote.getAccepted())) {
            throw new BadRequestException("Cannot edit a quote the commuter has already responded to");
        }
        if (quote.isCancelled()) {
            throw new BadRequestException("Cannot edit a cancelled quote");
        }

        quote.setAmount(req.amount());
        if (req.driverNote() != null) quote.setDriverNote(req.driverNote());

        // Keep the trip's quoted amount in sync
        tripRepository.findById(quote.getReferenceId()).ifPresent(trip -> {
            trip.setQuotedAmount(req.amount());
            tripRepository.save(trip);
        });

        return QuoteDto.from(quoteRepository.save(quote));
    }

    /** Driver cancels/retracts their pending quote. */
    @Transactional
    public void cancelQuote(String email, String quoteId) {
        User driver = getDriver(email);
        Quote quote = findQuote(quoteId);

        if (quote.getCreatedByDriver() == null ||
                !quote.getCreatedByDriver().getId().equals(driver.getId())) {
            throw new ResourceNotFoundException("Quote not found");
        }
        if (Boolean.TRUE.equals(quote.getAccepted()) || Boolean.FALSE.equals(quote.getAccepted())) {
            throw new BadRequestException("Cannot cancel a quote the commuter has already responded to");
        }

        quote.setCancelled(true);

        // Return trip to PENDING_QUOTE so other drivers can quote it
        tripRepository.findById(quote.getReferenceId()).ifPresent(trip -> {
            if (trip.getStatus() == TripBooking.TripStatus.QUOTE_SENT) {
                trip.setStatus(TripBooking.TripStatus.PENDING_QUOTE);
                trip.setDriver(null);
                trip.setDriverName(null);
                trip.setVehicle(null);
                trip.setVehicleInfo(null);
                trip.setVehiclePlate(null);
                trip.setQuotedAmount(null);
                tripRepository.save(trip);
            }
        });

        quoteRepository.save(quote);
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

    private Quote findQuote(String id) {
        return quoteRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found: " + id));
    }
}
