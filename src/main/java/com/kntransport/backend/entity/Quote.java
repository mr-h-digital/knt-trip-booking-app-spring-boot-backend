package com.kntransport.backend.entity;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The UUID of the TripBooking or LiftClub this quote is for. */
    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private ReferenceType referenceType;

    @Column(nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_cycle")
    private LiftClub.PaymentCycle paymentCycle;

    @Column(name = "driver_note", length = 1000)
    private String driverNote = "";

    @Column(nullable = true)
    private Boolean accepted;

    /** Driver who created this quote (null for admin-created quotes). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_driver_id")
    private User createdByDriver;

    /** True when the driver has retracted this quote before the commuter responded. */
    @Column(nullable = false)
    private boolean cancelled = false;

    public enum ReferenceType { TRIP, LIFT_CLUB }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

    public ReferenceType getReferenceType() { return referenceType; }
    public void setReferenceType(ReferenceType referenceType) { this.referenceType = referenceType; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LiftClub.PaymentCycle getPaymentCycle() { return paymentCycle; }
    public void setPaymentCycle(LiftClub.PaymentCycle paymentCycle) { this.paymentCycle = paymentCycle; }

    public String getDriverNote() { return driverNote; }
    public void setDriverNote(String driverNote) { this.driverNote = driverNote; }

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }

    public User getCreatedByDriver() { return createdByDriver; }
    public void setCreatedByDriver(User createdByDriver) { this.createdByDriver = createdByDriver; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
