package com.kntransport.backend.dto;

/** Driver earnings summary returned by GET /api/driver/earnings */
public record DriverEarningsDto(
        double totalEarnings,
        long   completedTrips,
        long   confirmedTrips,
        long   inProgressTrips,
        double averageEarningsPerTrip
) {}
