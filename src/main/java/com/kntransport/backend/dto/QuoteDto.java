package com.kntransport.backend.dto;

import com.kntransport.backend.entity.Quote;

public record QuoteDto(
        String id,
        String referenceId,
        String referenceType,
        double amount,
        String paymentCycle,
        String driverNote,
        String driverName,
        boolean cancelled
) {
    public static QuoteDto from(Quote q) {
        return new QuoteDto(
                q.getId().toString(),
                q.getReferenceId().toString(),
                q.getReferenceType().name(),
                q.getAmount(),
                q.getPaymentCycle() != null ? q.getPaymentCycle().name() : null,
                q.getDriverNote() != null ? q.getDriverNote() : "",
                q.getCreatedByDriver() != null ? q.getCreatedByDriver().getName() : null,
                q.isCancelled()
        );
    }
}
