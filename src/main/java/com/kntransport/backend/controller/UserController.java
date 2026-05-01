package com.kntransport.backend.controller;

import com.kntransport.backend.dto.UpdateProfileRequest;
import com.kntransport.backend.dto.UserDto;
import com.kntransport.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDto getProfile(@AuthenticationPrincipal UserDetails principal) {
        return userService.getProfile(principal.getUsername());
    }

    @PutMapping("/me")
    public UserDto updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getUsername(), request);
    }

    @PostMapping("/me/avatar")
    public UserDto uploadAvatar(
            @AuthenticationPrincipal UserDetails principal,
            @RequestPart("avatar") MultipartFile file) throws IOException {
        return userService.uploadAvatar(principal.getUsername(), file);
    }

    /** Records that the authenticated user has accepted the current Terms & Privacy Policy. */
    @PatchMapping("/me/accept-terms")
    public UserDto acceptTerms(@AuthenticationPrincipal UserDetails principal) {
        return userService.acceptTerms(principal.getUsername());
    }
}
