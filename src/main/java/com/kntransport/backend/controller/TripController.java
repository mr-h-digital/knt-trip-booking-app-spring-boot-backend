package com.kntransport.backend.controller;

import com.kntransport.backend.dto.CancelTripRequest;
import com.kntransport.backend.dto.CreateTripRequest;
import com.kntransport.backend.dto.DriverLocationDto;
import com.kntransport.backend.dto.PagedResponse;
import com.kntransport.backend.dto.RateTripRequest;
import com.kntransport.backend.dto.TripBookingDto;
import com.kntransport.backend.service.DriverLocationService;
import com.kntransport.backend.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService           tripService;
    private final DriverLocationService locationService;

    public TripController(TripService tripService, DriverLocationService locationService) {
        this.tripService     = tripService;
        this.locationService = locationService;
    }

    @GetMapping
    public PagedResponse<TripBookingDto> getMyTrips(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tripService.getMyTrips(principal.getUsername(), page, size);
    }

    @GetMapping("/{id}")
    public TripBookingDto getTrip(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id) {
        return tripService.getTrip(principal.getUsername(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripBookingDto createTrip(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateTripRequest request) {
        return tripService.createTrip(principal.getUsername(), request);
    }

    @PatchMapping("/{id}/cancel")
    public TripBookingDto cancelTrip(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id,
            @Valid @RequestBody CancelTripRequest request) {
        return tripService.cancelTrip(principal.getUsername(), id, request);
    }

    @PostMapping("/{id}/rate")
    public TripBookingDto rateTrip(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id,
            @Valid @RequestBody RateTripRequest request) {
        return tripService.rateTrip(principal.getUsername(), id, request);
    }

    /** Returns the driver's last known position for an in-progress trip. */
    @GetMapping("/{id}/location")
    public DriverLocationDto getLocation(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id) {
        return locationService.getLocation(principal.getUsername(), id);
    }
}
