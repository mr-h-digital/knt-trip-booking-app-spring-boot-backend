package com.kntransport.backend.controller;

import com.kntransport.backend.dto.NotificationDto;
import com.kntransport.backend.dto.PagedResponse;
import com.kntransport.backend.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PagedResponse<NotificationDto> getNotifications(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return notificationService.getNotifications(principal.getUsername(), page, size);
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String id) {
        notificationService.markRead(principal.getUsername(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal UserDetails principal) {
        notificationService.markAllRead(principal.getUsername());
    }
}
