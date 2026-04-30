package com.kntransport.backend.service;

import com.kntransport.backend.dto.QuoteAcceptRequest;
import com.kntransport.backend.dto.QuoteDto;
import com.kntransport.backend.entity.LiftClub;
import com.kntransport.backend.entity.Quote;
import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.ResourceNotFoundException;
import com.kntransport.backend.repository.LiftClubRepository;
import com.kntransport.backend.repository.QuoteRepository;
import com.kntransport.backend.repository.TripBookingRepository;
import com.kntransport.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class QuoteService {

    private final QuoteRepository        quoteRepository;
    private final TripBookingRepository  tripRepository;
    private final LiftClubRepository     liftClubRepository;
    private final UserRepository         userRepository;

    public QuoteService(QuoteRepository quoteRepository,
                        TripBookingRepository tripRepository,
                        LiftClubRepository liftClubRepository,
                        UserRepository userRepository) {
        this.quoteRepository    = quoteRepository;
        this.tripRepository     = tripRepository;
        this.liftClubRepository = liftClubRepository;
        this.userRepository     = userRepository;
    }

    public QuoteDto getQuote(String id) {
        return QuoteDto.from(findQuote(id));
    }

    @Transactional
    public QuoteDto respondToQuote(String id, QuoteAcceptRequest req) {
        // Use the fetch-join query so createdByDriver is never a lazy proxy
        Quote quote = quoteRepository.findByIdWithDriver(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found: " + id));

        quote.setAccepted(req.accepted());

        if (req.paymentCycle() != null && !req.paymentCycle().isBlank()) {
            quote.setPaymentCycle(LiftClub.PaymentCycle.valueOf(req.paymentCycle().toUpperCase()));
        }

        quoteRepository.save(quote);

        if (quote.getReferenceType() == Quote.ReferenceType.TRIP) {
            tripRepository.findById(quote.getReferenceId()).ifPresent(trip -> {
                if (req.accepted()) {
                    // Re-fetch the driver fully (including currentVehicle) to avoid lazy-load issues
                    User driver = quote.getCreatedByDriver() != null
                            ? userRepository.findById(quote.getCreatedByDriver().getId()).orElse(null)
                            : null;

                    trip.setDriver(driver);
                    if (driver != null) {
                        trip.setDriverName(driver.getName());
                        if (driver.getCurrentVehicle() != null) {
                            trip.setVehicle(driver.getCurrentVehicle());
                            trip.setVehicleInfo(driver.getCurrentVehicle().getMake() + " "
                                    + driver.getCurrentVehicle().getModel() + " — "
                                    + driver.getCurrentVehicle().getColour());
                            trip.setVehiclePlate(driver.getCurrentVehicle().getPlate());
                        }
                    }

                    trip.setQuotedAmount(quote.getAmount());
                    trip.setStatus(TripBooking.TripStatus.QUOTE_ACCEPTED);

                    // Cancel all other pending quotes for this trip
                    quoteRepository
                            .findAllByReferenceIdAndReferenceTypeAndCancelledFalse(
                                    trip.getId(), Quote.ReferenceType.TRIP)
                            .forEach(other -> {
                                if (!other.getId().equals(quote.getId())) {
                                    other.setCancelled(true);
                                    quoteRepository.save(other);
                                }
                            });
                } else {
                    // Declined — keep QUOTE_SENT if other active quotes remain, else PENDING_QUOTE
                    long remaining = quoteRepository
                            .findAllByReferenceIdAndReferenceTypeAndCancelledFalse(
                                    trip.getId(), Quote.ReferenceType.TRIP)
                            .stream()
                            .filter(q -> !q.getId().equals(quote.getId()))
                            .count();
                    trip.setStatus(remaining > 0
                            ? TripBooking.TripStatus.QUOTE_SENT
                            : TripBooking.TripStatus.PENDING_QUOTE);
                }
                tripRepository.save(trip);
            });
        } else {
            liftClubRepository.findById(quote.getReferenceId()).ifPresent(lc -> {
                lc.setStatus(req.accepted()
                        ? LiftClub.LiftClubStatus.ACTIVE
                        : LiftClub.LiftClubStatus.OPEN);
                if (req.accepted()) {
                    lc.setQuotedAmount(quote.getAmount());
                    if (req.paymentCycle() != null && !req.paymentCycle().isBlank()) {
                        lc.setPaymentCycle(LiftClub.PaymentCycle.valueOf(req.paymentCycle().toUpperCase()));
                    }
                }
                liftClubRepository.save(lc);
            });
        }

        return QuoteDto.from(quote);
    }

    private Quote findQuote(String id) {
        return quoteRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found: " + id));
    }
}
