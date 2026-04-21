package com.kntransport.backend.dto;

import com.kntransport.backend.entity.User;

public record UserDto(
        String id,
        String name,
        String email,
        String phone,
        String role,
        String avatarUrl
) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId().toString(),
                u.getName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole().name(),
                u.getAvatarUrl()
        );
    }
}
