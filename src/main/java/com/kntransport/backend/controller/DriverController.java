package com.kntransport.backend.controller;

import com.kntransport.backend.dto.*;
import com.kntransport.backend.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    /** All trips assigned to the authenticated driver, newest first. */
    @GetMapping("/trips")
    public PagedResponse<TripBookingDto> getMyTrips(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return driverService.getMyTrips(principal.getUsername(), page, size);
    }

    /** Single trip detail for the authenticated driver. */
    @GetMapping("/trips/{id}")
    public TripBookingDto getMyTrip(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id) {
        return driverService.getMyTrip(principal.getUsername(), id);
    }

    /**
     * Driver updates a trip's status.
     * Body: { "status": "IN_PROGRESS" | "COMPLETED" | "CANCELLED" }
     */
    @PatchMapping("/trips/{id}/status")
    public TripBookingDto updateStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id,
            @Valid @RequestBody UpdateTripStatusRequest request) {
        return driverService.updateStatus(principal.getUsername(), id, request);
    }

    /** Driver earnings and trip-count summary. */
    @GetMapping("/earnings")
    public DriverEarningsDto getEarnings(@AuthenticationPrincipal UserDetails principal) {
        return driverService.getEarnings(principal.getUsername());
    }
}
