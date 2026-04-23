package com.kntransport.backend.dto;

/** Body for admin assigning a vehicle to a driver (vehicleId null = unassign). */
public record AssignVehicleRequest(
        String vehicleId
) {}
