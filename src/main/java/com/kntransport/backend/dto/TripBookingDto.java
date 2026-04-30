package com.kntransport.backend.dto;

import com.kntransport.backend.entity.Quote;
import com.kntransport.backend.entity.TripBooking;

public record TripBookingDto(
        String id,
        String pickupAddress,
        String dropAddress,
        String date,
        String time,
        int passengers,
        String notes,
        String status,
        Double quotedAmount,
        String commuterName,
        String commuterPhone,
        String driverName,
        String driverId,
        // ── Vehicle details (shown to commuter like Uber — what car is coming) ─
        String vehicleId,
        String vehicleMake,
        String vehicleModel,
        String vehicleColour,
        String vehiclePlate,
        String vehicleType,
        String vehiclePhotoUrl,
        String driverAvatarUrl,
        // ── Legacy string fields (kept for backwards compat) ──────────────────
        String vehicleInfo,
        Integer rating,
        // ── Driver's own quote for this trip (null unless explicitly loaded) ──
        QuoteDto myQuote
) {
    public static TripBookingDto from(TripBooking t) {
        return fromWithMyQuote(t, null);
    }

    public static TripBookingDto fromWithMyQuote(TripBooking t, Quote myQuote) {
        var v = t.getVehicle();
        return new TripBookingDto(
                t.getId().toString(),
                t.getPickupAddress(),
                t.getDropAddress(),
                t.getDate().toString(),
                t.getTime().toString(),
                t.getPassengers(),
                t.getNotes() != null ? t.getNotes() : "",
                t.getStatus().name(),
                t.getQuotedAmount(),
                t.getCommuter() != null ? t.getCommuter().getName() : null,
                t.getCommuter() != null ? t.getCommuter().getPhone() : null,
                t.getDriverName(),
                t.getDriver() != null ? t.getDriver().getId().toString() : null,
                v != null ? v.getId().toString()                : null,
                v != null ? v.getMake()                         : null,
                v != null ? v.getModel()                        : null,
                v != null ? v.getColour()                       : null,
                v != null ? v.getPlate()                        : t.getVehiclePlate(),
                v != null ? v.getVehicleType().name()           : null,
                v != null ? v.getPhotoUrl()                     : null,
                t.getDriver() != null ? t.getDriver().getAvatarUrl() : null,
                t.getVehicleInfo(),
                t.getRating(),
                myQuote != null ? QuoteDto.from(myQuote) : null
        );
    }
}
