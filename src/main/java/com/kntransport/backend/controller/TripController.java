package com.kntransport.backend.controller;

import com.kntransport.backend.dto.CreateTripRequest;
import com.kntransport.backend.dto.PagedResponse;
import com.kntransport.backend.dto.TripBookingDto;
import com.kntransport.backend.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
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
}
