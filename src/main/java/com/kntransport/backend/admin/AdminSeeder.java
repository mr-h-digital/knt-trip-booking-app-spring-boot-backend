package com.kntransport.backend.admin;

import com.kntransport.backend.entity.User;
import com.kntransport.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the default K&T Transport admin account on every startup if it does
 * not already exist.  Credentials are driven by environment variables so they
 * are never hard-coded in the deployed JAR:
 *
 *   ADMIN_EMAIL    (default: admin@ktransport.co.za)
 *   ADMIN_PASSWORD (default: KnT@Admin2026!)
 *   ADMIN_NAME     (default: K&T Admin)
 *   ADMIN_PHONE    (default: +27000000000)
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email    = System.getenv().getOrDefault("ADMIN_EMAIL",    "admin@ktransport.co.za");
        String password = System.getenv().getOrDefault("ADMIN_PASSWORD", "KnT@Admin2026!");
        String name     = System.getenv().getOrDefault("ADMIN_NAME",     "K&T Admin");
        String phone    = System.getenv().getOrDefault("ADMIN_PHONE",    "+27000000000");

        if (userRepository.existsByEmail(email)) {
            log.info("Admin account already exists: {}", email);
            return;
        }

        User admin = new User();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPhone(phone);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);

        log.info("Admin account created: {}", email);
    }
}
