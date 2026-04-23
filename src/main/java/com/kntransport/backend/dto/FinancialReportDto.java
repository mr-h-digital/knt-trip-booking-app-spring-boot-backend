package com.kntransport.backend.dto;

import java.util.List;

/**
 * Financial report payload for the accountant.
 * Covers a date range supplied by the admin via query params.
 */
public record FinancialReportDto(

    String reportFrom,
    String reportTo,
    String generatedAt,
    String generatedBy,

    // ── Summary ───────────────────────────────────────────────────────────────
    double grossRevenue,
    long   invoiceCount,
    double averageInvoiceValue,
    double highestInvoiceValue,
    double lowestInvoiceValue,

    // ── Payment cycle breakdown ───────────────────────────────────────────────
    double onceOffRevenue,
    double weeklyRevenue,
    double fortnightlyRevenue,
    double monthlyRevenue,

    // ── Line items ────────────────────────────────────────────────────────────
    List<LineItem> lineItems

) {
    public record LineItem(
        String date,
        String referenceType,   // TRIP or LIFT_CLUB
        String referenceId,
        String clientName,
        String clientPhone,
        String paymentCycle,    // ONCE_OFF | WEEKLY | FORTNIGHTLY | MONTHLY
        double amount,
        boolean accepted
    ) {}
}
