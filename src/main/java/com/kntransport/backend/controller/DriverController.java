package com.kntransport.backend.controller;

import com.kntransport.backend.dto.*;
import com.kntransport.backend.service.DriverLocationService;
import com.kntransport.backend.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final DriverService         driverService;
    private final DriverLocationService locationService;

    public DriverController(DriverService driverService, DriverLocationService locationService) {
        this.driverService   = driverService;
        this.locationService = locationService;
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

    /** Driver cancels a trip (e.g. unable to fulfil). */
    @PatchMapping("/trips/{id}/cancel")
    public TripBookingDto cancelTrip(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id,
            @Valid @RequestBody CancelTripRequest request) {
        return driverService.cancelTrip(principal.getUsername(), id, request);
    }

    /** Driver pushes their current GPS position for an in-progress trip. */
    @PutMapping("/trips/{id}/location")
    public ResponseEntity<Void> updateLocation(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id,
            @Valid @RequestBody UpdateLocationRequest request) {
        locationService.updateLocation(principal.getUsername(), id, request);
        return ResponseEntity.ok().build();
    }

    /** Driver earnings and trip-count summary. */
    @GetMapping("/earnings")
    public DriverEarningsDto getEarnings(@AuthenticationPrincipal UserDetails principal) {
        return driverService.getEarnings(principal.getUsername());
    }
}
