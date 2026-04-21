package com.kntransport.backend.dto;

public record AuthResponse(String token, String role, UserDto user) {}
