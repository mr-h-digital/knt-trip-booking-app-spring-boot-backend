package com.kntransport.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DriverQuoteRequest(
        @NotNull @DecimalMin("0.01") Double amount,
        String driverNote
) {}
