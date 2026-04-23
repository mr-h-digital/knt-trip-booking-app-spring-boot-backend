package com.kntransport.backend.dto;

import java.util.List;

/** Business analytics dashboard payload returned to the admin. */
public record AnalyticsDto(

    // ── User stats ────────────────────────────────────────────────────────────
    long totalUsers,
    long totalCommuters,
    long totalDrivers,

    // ── Trip stats ────────────────────────────────────────────────────────────
    long totalTrips,
    long pendingQuoteTrips,
    long confirmedTrips,
    long completedTrips,
    long cancelledTrips,

    // ── Lift club stats ───────────────────────────────────────────────────────
    long totalLiftClubs,
    long openLiftClubs,
    long activeLiftClubs,

    // ── Financial totals ──────────────────────────────────────────────────────
    double totalRevenue,
    double totalOutstanding,
    double averageTripValue,

    // ── Breakdown lists ───────────────────────────────────────────────────────
    List<MonthlyRevenue> revenueByMonth,
    List<TripStatusBreakdown> tripStatusBreakdown

) {
    public record MonthlyRevenue(String month, double revenue, long tripCount) {}
    public record TripStatusBreakdown(String status, long count, double percentage) {}
}
