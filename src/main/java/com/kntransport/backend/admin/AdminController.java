package com.kntransport.backend.admin;

import com.kntransport.backend.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ── User management ───────────────────────────────────────────────────────

    /** List all users, optionally filtered by role: COMMUTER | DRIVER | ADMIN */
    @GetMapping("/users")
    public PagedResponse<UserDto> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return PagedResponse.from(adminService.listUsers(role, page, size), u -> u);
    }

    @GetMapping("/users/{id}")
    public UserDto getUser(@PathVariable String id) {
        return adminService.getUser(id);
    }

    /** Create any user type — COMMUTER, DRIVER, or ADMIN. */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody AdminUserRequest request) {
        return adminService.createUser(request);
    }

    /** Full update of any user's details including role promotion/demotion. */
    @PutMapping("/users/{id}")
    public UserDto updateUser(
            @PathVariable String id,
            @Valid @RequestBody AdminUserRequest request) {
        return adminService.updateUser(id, request);
    }

    /** Delete a commuter or driver account (cannot delete ADMIN). */
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String id) {
        adminService.deleteUser(id);
    }

    // ── Vehicle fleet management ──────────────────────────────────────────────

    /**
     * List the fleet. Optional ?active=true to return only active vehicles.
     */
    @GetMapping("/vehicles")
    public Page<VehicleDto> listVehicles(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listVehicles(active, page, size);
    }

    @GetMapping("/vehicles/{id}")
    public VehicleDto getVehicle(@PathVariable String id) {
        return adminService.getVehicle(id);
    }

    /** Add a new vehicle to the fleet. */
    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto createVehicle(@Valid @RequestBody VehicleRequest request) {
        return adminService.createVehicle(request);
    }

    /** Update a vehicle's details. */
    @PutMapping("/vehicles/{id}")
    public VehicleDto updateVehicle(
            @PathVariable String id,
            @Valid @RequestBody VehicleRequest request) {
        return adminService.updateVehicle(id, request);
    }

    /** Deactivate (soft-delete) a vehicle and unassign from any driver. */
    @DeleteMapping("/vehicles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateVehicle(@PathVariable String id) {
        adminService.deactivateVehicle(id);
    }

    /** Reactivate a previously deactivated vehicle. */
    @PatchMapping("/vehicles/{id}/reactivate")
    public VehicleDto reactivateVehicle(@PathVariable String id) {
        return adminService.reactivateVehicle(id);
    }

    /** Upload or replace a vehicle's photo. */
    @PostMapping("/vehicles/{id}/photo")
    public VehicleDto uploadVehiclePhoto(
            @PathVariable String id,
            @RequestPart("photo") MultipartFile file) throws IOException {
        return adminService.uploadVehiclePhoto(id, file);
    }

    /**
     * Assign a vehicle to a driver.
     * Body: { "vehicleId": "uuid" } — pass null/empty vehicleId to unassign.
     */
    @PatchMapping("/drivers/{driverId}/assign-vehicle")
    public UserDto assignVehicleToDriver(
            @PathVariable String driverId,
            @RequestBody AssignVehicleRequest request) {
        return adminService.assignVehicleToDriver(driverId, request);
    }

    // ── Trip management (admin view) ──────────────────────────────────────────

    /** All trips across all commuters. */
    @GetMapping("/trips")
    public PagedResponse<TripBookingDto> listAllTrips(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listAllTrips(page, size);
    }

    /** Assign a driver to a trip. Body: { "driverId": "uuid" } */
    @PatchMapping("/trips/{tripId}/assign-driver")
    public TripBookingDto assignDriver(
            @PathVariable String tripId,
            @Valid @RequestBody AssignDriverRequest request) {
        return adminService.assignDriver(tripId, request);
    }

    // ── Analytics dashboard ───────────────────────────────────────────────────

    @GetMapping("/analytics")
    public AnalyticsDto getAnalytics() {
        return adminService.getAnalytics();
    }

    // ── Financial report ──────────────────────────────────────────────────────

    /**
     * Generate a financial report for the accountant.
     * ?from=2026-01-01&to=2026-12-31
     * The from/to are informational labels stored in the report header;
     * the current implementation returns all decided quotes.
     * A future iteration can filter by trip/quote date once those
     * have a settled date range query.
     */
    @GetMapping("/financial-report")
    public FinancialReportDto financialReport(
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to,
            @AuthenticationPrincipal UserDetails principal) {
        return adminService.generateFinancialReport(from, to, principal.getUsername());
    }
}
