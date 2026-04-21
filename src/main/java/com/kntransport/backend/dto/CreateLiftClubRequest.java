package com.kntransport.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateLiftClubRequest(
        @NotBlank String title,
        @NotBlank String pickupArea,
        @NotBlank String dropArea,
        @NotBlank String departureTime,
        String returnTime,
        @NotEmpty List<String> daysOfWeek,
        @NotNull @Min(2) Integer maxPassengers,
        String description
) {}
