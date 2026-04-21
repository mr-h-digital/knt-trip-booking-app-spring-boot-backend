package com.kntransport.backend.dto;

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
        String driverName,
        String vehicleInfo,
        String vehiclePlate
) {
    public static TripBookingDto from(TripBooking t) {
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
                t.getDriverName(),
                t.getVehicleInfo(),
                t.getVehiclePlate()
        );
    }
}
