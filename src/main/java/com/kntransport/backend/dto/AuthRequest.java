package com.kntransport.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    public static class Login {
        @NotBlank @Email
        public String email;

        @NotBlank @Size(min = 6)
        public String password;
    }

    public static class Register {
        @NotBlank
        public String name;

        @NotBlank @Email
        public String email;

        @NotBlank
        public String phone;

        @NotBlank @Size(min = 6)
        public String password;
    }
}
