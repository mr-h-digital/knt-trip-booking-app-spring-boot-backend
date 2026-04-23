package com.kntransport.backend.dto;

import jakarta.validation.constraints.*;

public record VehicleRequest(
        @NotBlank String make,
        @NotBlank String model,
        @NotBlank String colour,
        @NotBlank String plate,
        @Min(1990) @Max(2100) int year,
        /** SEDAN | SUV | MINIBUS | BUS  (defaults to MINIBUS if null) */
        String vehicleType,
        String photoUrl,
        String notes
) {}
