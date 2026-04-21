package com.kntransport.backend.service;

import com.kntransport.backend.dto.UpdateProfileRequest;
import com.kntransport.backend.dto.UserDto;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.BadRequestException;
import com.kntransport.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// TODO: Replace local filesystem storage with S3/Cloudflare R2 before
// enabling avatar uploads in production. Railway containers are ephemeral —
// files written to disk are lost on every redeploy.

@Service
public class UserService {

    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        if (file.isEmpty()) {
            throw new BadRequestException("Avatar file is empty");
        }

        // Block uploads on Railway until cloud storage (S3/R2) is configured,
        // because the Railway filesystem is ephemeral and files will not persist.
        if (System.getenv("RAILWAY_ENVIRONMENT") != null) {
            throw new BadRequestException(
                "Avatar file upload is not yet supported on this server. " +
                "Please provide an avatar URL via profile update instead."
            );
        }

        Path dir = Paths.get(uploadDir, "avatars").toAbsolutePath();
        Files.createDirectories(dir);

        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        file.transferTo(dir.resolve(filename));

        User user = getByEmail(email);
        user.setAvatarUrl("/uploads/avatars/" + filename);
        return UserDto.from(userRepository.save(user));
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
