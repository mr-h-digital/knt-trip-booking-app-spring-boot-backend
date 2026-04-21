package com.kntransport.backend.service;

import com.kntransport.backend.dto.AuthRequest;
import com.kntransport.backend.dto.AuthResponse;
import com.kntransport.backend.dto.UserDto;
import com.kntransport.backend.entity.User;
import com.kntransport.backend.exception.BadRequestException;
import com.kntransport.backend.repository.UserRepository;
import com.kntransport.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponse register(AuthRequest.Register req) {
        if (userRepository.existsByEmail(req.email)) {
            throw new BadRequestException("Email already registered");
        }
        User user = new User();
        user.setName(req.name);
        user.setEmail(req.email);
        user.setPhone(req.phone);
        user.setPassword(passwordEncoder.encode(req.password));
        user.setRole(User.Role.COMMUTER);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(AuthRequest.Login req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email, req.password)
        );
        User user = userRepository.findByEmail(req.email).orElseThrow();
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails, Map.of("role", user.getRole().name()));
        return new AuthResponse(token, user.getRole().name(), UserDto.from(user));
    }
}
