package com.kntransport.backend.dto;

import com.kntransport.backend.entity.User;

public record UserDto(
        String id,
        String name,
        String email,
        String phone,
        String role,
        String avatarUrl,
        // ── Driver vehicle snapshot (null for non-drivers / unassigned) ────────
        String currentVehicleId,
        String currentVehicleMake,
        String currentVehicleModel,
        String currentVehicleColour,
        String currentVehiclePlate,
        String currentVehicleType,
        String currentVehiclePhotoUrl
) {
    public static UserDto from(User u) {
        var v = u.getCurrentVehicle();
        return new UserDto(
                u.getId().toString(),
                u.getName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole().name(),
                u.getAvatarUrl(),
                v != null ? v.getId().toString()      : null,
                v != null ? v.getMake()               : null,
                v != null ? v.getModel()              : null,
                v != null ? v.getColour()             : null,
                v != null ? v.getPlate()              : null,
                v != null ? v.getVehicleType().name() : null,
                v != null ? v.getPhotoUrl()           : null
        );
    }
}
