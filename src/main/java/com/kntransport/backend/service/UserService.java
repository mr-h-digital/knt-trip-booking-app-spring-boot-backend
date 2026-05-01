package com.kntransport.backend.service;

import com.kntransport.backend.dto.UpdateProfileRequest;
import com.kntransport.backend.dto.UserDto;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.BadRequestException;
import com.kntransport.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StorageService storageService;

    public UserService(UserRepository userRepository, StorageService storageService) {
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    public UserDto getProfile(String email) {
        return UserDto.from(getByEmail(email));
    }

    public UserDto updateProfile(String email, UpdateProfileRequest req) {
        User user = getByEmail(email);

        if (!user.getEmail().equals(req.email()) && userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("Email already in use");
        }

        user.setName(req.name());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        return UserDto.from(userRepository.save(user));
    }

    public UserDto uploadAvatar(String email, MultipartFile file) throws IOException {
        User user = getByEmail(email);
        // Delete the old file if it was a UUID-based URL (migration cleanup)
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().contains(user.getId().toString())) {
            storageService.deleteByUrl(user.getAvatarUrl());
        }
        // Use the user's ID as the filename — overwrites in R2 on every upload.
        // Append a timestamp so Coil treats each upload as a distinct URL and bypasses its cache.
        String url = storageService.store("avatars", file, user.getId().toString())
                + "?v=" + System.currentTimeMillis();
        user.setAvatarUrl(url);
        return UserDto.from(userRepository.save(user));
    }

    public UserDto acceptTerms(String email) {
        User user = getByEmail(email);
        user.setTermsAcceptedAt(java.time.Instant.now());
        return UserDto.from(userRepository.save(user));
    }
}
