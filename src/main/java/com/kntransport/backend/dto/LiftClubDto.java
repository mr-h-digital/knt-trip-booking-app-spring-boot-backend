package com.kntransport.backend.dto;

import com.kntransport.backend.entity.LiftClub;
import java.util.List;

public record LiftClubDto(
        String id,
        String title,
        String pickupArea,
        String dropArea,
        String departureTime,
        List<String> daysOfWeek,
        int maxPassengers,
        long subscriberCount,
        String status,
        Double quotedAmount,
        String paymentCycle,
        String description
) {
    public static LiftClubDto from(LiftClub lc, long subscriberCount) {
        return new LiftClubDto(
                lc.getId().toString(),
                lc.getTitle(),
                lc.getPickupArea(),
                lc.getDropArea(),
                lc.getDepartureTime().toString(),
                lc.getDaysOfWeek(),
                lc.getMaxPassengers(),
                subscriberCount,
                lc.getStatus().name(),
                lc.getQuotedAmount(),
                lc.getPaymentCycle() != null ? lc.getPaymentCycle().name() : null,
                lc.getDescription() != null ? lc.getDescription() : ""
        );
    }
}
