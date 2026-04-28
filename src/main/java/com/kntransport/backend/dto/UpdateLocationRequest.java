package com.kntransport.backend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateLocationRequest(
        @NotNull Double latitude,
        @NotNull Double longitude
) {}
