package com.kntransport.backend.controller;

import com.kntransport.backend.dto.CreateLiftClubRequest;
import com.kntransport.backend.dto.LiftClubDto;
import com.kntransport.backend.dto.PagedResponse;
import com.kntransport.backend.service.LiftClubService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lift-clubs")
public class LiftClubController {

    private final LiftClubService liftClubService;

    public LiftClubController(LiftClubService liftClubService) {
        this.liftClubService = liftClubService;
    }

    @GetMapping
    public PagedResponse<LiftClubDto> getLiftClubs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return liftClubService.getLiftClubs(page, size);
    }

    @GetMapping("/{id}")
    public LiftClubDto getLiftClub(@PathVariable String id) {
        return liftClubService.getLiftClub(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LiftClubDto createLiftClub(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateLiftClubRequest request) {
        return liftClubService.createLiftClub(principal.getUsername(), request);
    }

    @PostMapping("/{id}/subscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id) {
        liftClubService.subscribe(principal.getUsername(), id);
    }
}
