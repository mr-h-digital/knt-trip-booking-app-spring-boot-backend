package com.kntransport.backend.service;

import com.kntransport.backend.dto.QuoteAcceptRequest;
import com.kntransport.backend.dto.QuoteDto;
import com.kntransport.backend.entity.LiftClub;
import com.kntransport.backend.entity.Quote;
import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.exception.ResourceNotFoundException;
import com.kntransport.backend.repository.LiftClubRepository;
import com.kntransport.backend.repository.QuoteRepository;
import com.kntransport.backend.repository.TripBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final TripBookingRepository tripRepository;
    private final LiftClubRepository liftClubRepository;

    public QuoteService(QuoteRepository quoteRepository,
                        TripBookingRepository tripRepository,
                        LiftClubRepository liftClubRepository) {
        this.quoteRepository = quoteRepository;
        this.tripRepository = tripRepository;
        this.liftClubRepository = liftClubRepository;
    }

    public QuoteDto getQuote(String id) {
        return QuoteDto.from(findQuote(id));
    }

    @Transactional
    public QuoteDto respondToQuote(String id, QuoteAcceptRequest req) {
        Quote quote = findQuote(id);
        quote.setAccepted(req.accepted());

        if (req.paymentCycle() != null && !req.paymentCycle().isBlank()) {
            quote.setPaymentCycle(LiftClub.PaymentCycle.valueOf(req.paymentCycle().toUpperCase()));
        }

        quoteRepository.save(quote);

        // Update the related entity's status based on acceptance
        if (quote.getReferenceType() == Quote.ReferenceType.TRIP) {
            tripRepository.findById(quote.getReferenceId()).ifPresent(trip -> {
                trip.setStatus(req.accepted()
                        ? TripBooking.TripStatus.QUOTE_ACCEPTED
                        : TripBooking.TripStatus.PENDING_QUOTE);
                if (req.accepted()) {
                    trip.setQuotedAmount(quote.getAmount());
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
