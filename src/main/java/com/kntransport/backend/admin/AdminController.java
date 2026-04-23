package com.kntransport.backend.admin;

import com.kntransport.backend.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
    public Page<UserDto> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminService.listUsers(role, page, size);
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
