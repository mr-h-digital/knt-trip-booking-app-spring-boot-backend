package com.kntransport.backend.admin;

import com.kntransport.backend.entity.User;
import com.kntransport.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        upsert("info@mrhdigital.co.za", "Lee Hildebrandt", "0766871671", "password", User.Role.ADMIN);
        upsert("htaswill@gmail.com",    "Taswill Heynes",  "0787784182", "password", User.Role.DRIVER);
    }

    private void upsert(String email, String name, String phone, String rawPassword, User.Role role) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            log.info("Creating {} account: {}", role, email);
        } else {
            log.info("Updating {} account: {}", role, email);
        }
        user.setName(name);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
    }
}
