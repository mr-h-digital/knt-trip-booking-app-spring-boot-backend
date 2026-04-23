package com.kntransport.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Sent by the driver to advance a trip's status. */
public record UpdateTripStatusRequest(
        @NotBlank
        @Pattern(regexp = "IN_PROGRESS|COMPLETED|CANCELLED",
                 message = "Status must be IN_PROGRESS, COMPLETED, or CANCELLED")
        String status
) {}
