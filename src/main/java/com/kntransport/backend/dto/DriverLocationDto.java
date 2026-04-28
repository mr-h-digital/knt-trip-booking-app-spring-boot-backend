package com.kntransport.backend.dto;

import com.kntransport.backend.entity.DriverLocation;

import java.time.LocalDateTime;

public record DriverLocationDto(
        String tripId,
        Double latitude,
        Double longitude,
        LocalDateTime updatedAt
) {
    public static DriverLocationDto from(DriverLocation loc) {
        return new DriverLocationDto(
                loc.getTrip().getId().toString(),
                loc.getLatitude(),
                loc.getLongitude(),
                loc.getUpdatedAt()
        );
    }
}
