package com.kntransport.backend.dto;

import jakarta.validation.constraints.NotNull;

public record QuoteAcceptRequest(
        @NotNull Boolean accepted,
        String paymentCycle
) {}
