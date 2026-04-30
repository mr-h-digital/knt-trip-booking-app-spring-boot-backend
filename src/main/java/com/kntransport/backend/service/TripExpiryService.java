package com.kntransport.backend.service;

import com.kntransport.backend.entity.TripBooking;
import com.kntransport.backend.repository.TripBookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

/**
 * Nightly job that auto-expires trip requests whose travel date has passed
 * without ever being confirmed. Runs at 01:00 server time every day.
 *
 * Only cancels PENDING_QUOTE and QUOTE_SENT trips — confirmed/in-progress
 * trips are live bookings and must never be auto-cancelled.
 */
@Service
public class TripExpiryService {

    private final TripBookingRepository tripRepository;

    public TripExpiryService(TripBookingRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireLapsedTrips() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        EnumSet<TripBooking.TripStatus> expirableStatuses =
                EnumSet.of(TripBooking.TripStatus.PENDING_QUOTE, TripBooking.TripStatus.QUOTE_SENT);

        List<TripBooking> lapsed = tripRepository
                .findByStatusInAndDateBefore(expirableStatuses, yesterday);

        lapsed.forEach(trip -> trip.setStatus(TripBooking.TripStatus.CANCELLED));
        tripRepository.saveAll(lapsed);
    }
}
