package com.kntransport.backend.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lift_clubs")
public class LiftClub {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private String title;

    @Column(name = "pickup_area", nullable = false)
    private String pickupArea;

    @Column(name = "drop_area", nullable = false)
    private String dropArea;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "return_time")
    private LocalTime returnTime;

    @ElementCollection
    @CollectionTable(name = "lift_club_days", joinColumns = @JoinColumn(name = "lift_club_id"))
    @Column(name = "day_of_week")
    private List<String> daysOfWeek;

    @Column(name = "max_passengers", nullable = false)
    private int maxPassengers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LiftClubStatus status = LiftClubStatus.OPEN;

    @Column(name = "quoted_amount")
    private Double quotedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_cycle")
    private PaymentCycle paymentCycle;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "vehicle_info")
    private String vehicleInfo;

    @Column(length = 2000)
    private String description = "";

    public enum LiftClubStatus { OPEN, QUOTA_MET, QUOTE_SENT, ACTIVE, COMPLETED, CANCELLED }
    public enum PaymentCycle   { MONTHLY, WEEKLY, FORTNIGHTLY }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPickupArea() { return pickupArea; }
    public void setPickupArea(String pickupArea) { this.pickupArea = pickupArea; }

    public String getDropArea() { return dropArea; }
    public void setDropArea(String dropArea) { this.dropArea = dropArea; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public LocalTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalTime returnTime) { this.returnTime = returnTime; }

    public List<String> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public int getMaxPassengers() { return maxPassengers; }
    public void setMaxPassengers(int maxPassengers) { this.maxPassengers = maxPassengers; }

    public LiftClubStatus getStatus() { return status; }
    public void setStatus(LiftClubStatus status) { this.status = status; }

    public Double getQuotedAmount() { return quotedAmount; }
    public void setQuotedAmount(Double quotedAmount) { this.quotedAmount = quotedAmount; }

    public PaymentCycle getPaymentCycle() { return paymentCycle; }
    public void setPaymentCycle(PaymentCycle paymentCycle) { this.paymentCycle = paymentCycle; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getVehicleInfo() { return vehicleInfo; }
    public void setVehicleInfo(String vehicleInfo) { this.vehicleInfo = vehicleInfo; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
