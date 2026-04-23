package com.kntransport.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Represents a vehicle in the K&T Transport fleet.
 * The admin manages this list. Each driver can have one currently assigned vehicle.
 * Each trip records the vehicle used at the time (snapshot FK).
 */
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String make;           // e.g. Toyota

    @Column(nullable = false)
    private String model;          // e.g. HiAce

    @Column(nullable = false)
    private String colour;         // e.g. White

    @Column(nullable = false, unique = true)
    private String plate;          // e.g. CA 456 789

    @Column(nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType = VehicleType.MINIBUS;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String notes;

    public enum VehicleType { SEDAN, SUV, MINIBUS, BUS }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
