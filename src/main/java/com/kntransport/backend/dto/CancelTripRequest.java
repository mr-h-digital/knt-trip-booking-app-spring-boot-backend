package com.kntransport.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelTripRequest(
        @NotBlank String reason,
        String note
) {}
