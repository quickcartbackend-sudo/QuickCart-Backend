package com.quickcart.user.config;

import com.quickcart.user.entity.Role;
import com.quickcart.user.entity.User;
import com.quickcart.user.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       @Value("${quickcart.admin.email}") String adminEmail,
                       @Value("${quickcart.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        userRepository.save(new User("QuickCart Admin", adminEmail,
                passwordEncoder.encode(adminPassword), "9999999999", Role.ADMIN));
        log.info("Seeded ADMIN account {}", adminEmail);   // the password is never logged
    }
    // There is no admin registration endpoint. The admin phone is NOT NULL in the schema and the contract gives no value, so the seeder uses a placeholder matching the phone regex.
}