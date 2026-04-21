package com.kntransport.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTripRequest(
        @NotBlank String pickupAddress,
        @NotBlank String dropAddress,
        @NotBlank String date,
        @NotBlank String time,
        @NotNull @Min(1) Integer passengers,
        String notes
) {}
