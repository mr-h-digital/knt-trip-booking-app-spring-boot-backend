package com.kntransport.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for admin assigning a driver to a trip. */
public record AssignDriverRequest(
        @NotBlank String driverId
) {}
