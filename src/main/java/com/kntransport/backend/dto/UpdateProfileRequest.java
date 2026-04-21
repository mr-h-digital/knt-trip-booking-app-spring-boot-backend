package com.kntransport.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String phone
) {}
