package com.kntransport.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Used for admin creating or fully updating a user (commuter or driver). */
public record AdminUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String phone,
        /** Required only when creating. Ignored on update if blank. */
        @Size(min = 6) String password,
        /** COMMUTER | DRIVER | ADMIN */
        @NotBlank @Pattern(regexp = "COMMUTER|DRIVER|ADMIN") String role
) {}
