package com.kntransport.backend.dto;

import com.kntransport.backend.entity.Vehicle;

public record VehicleDto(
        String id,
        String make,
        String model,
        String colour,
        String plate,
        int    year,
        String vehicleType,
        String photoUrl,
        boolean active,
        String notes,
        /** The driver currently assigned this vehicle (null if unassigned). */
        String assignedDriverId,
        String assignedDriverName
) {
    public static VehicleDto from(Vehicle v, String driverId, String driverName) {
        return new VehicleDto(
                v.getId().toString(),
                v.getMake(),
                v.getModel(),
                v.getColour(),
                v.getPlate(),
                v.getYear(),
                v.getVehicleType().name(),
                v.getPhotoUrl(),
                v.isActive(),
                v.getNotes(),
                driverId,
                driverName
        );
    }
}
