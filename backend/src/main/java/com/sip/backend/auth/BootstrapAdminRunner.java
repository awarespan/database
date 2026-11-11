package com.sip.backend.auth;

import com.sip.backend.entity.User;
import com.sip.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Optional one-time bootstrap for a first administrator. No default credentials are supplied.
 * Set both SIP_BOOTSTRAP_ADMIN_USERNAME and SIP_BOOTSTRAP_ADMIN_PASSWORD in a private environment.
 */
@Component
public class BootstrapAdminRunner implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public BootstrapAdminRunner(UserRepository users, PasswordEncoder passwordEncoder,
            @Value("${SIP_BOOTSTRAP_ADMIN_USERNAME:}") String username,
            @Value("${SIP_BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("[BootstrapAdminRunner] Starting — username='" + username + "', password length=" + password.length());
        if (username.isBlank() || password.isBlank()) {
            System.out.println("[BootstrapAdminRunner] SKIP: username or password is blank");
            return;
        }
        if (password.length() < 12) {
            System.out.println("[BootstrapAdminRunner] SKIP: password too short (" + password.length() + " chars, need 12+)");
            return;
        }
        if (users.existsByUsername(username)) {
            System.out.println("[BootstrapAdminRunner] SKIP: user '" + username + "' already exists");
            return;
        }

        System.out.println("[BootstrapAdminRunner] Creating admin user: " + username);
        User admin = new User(UUID.randomUUID().toString(), username,
                passwordEncoder.encode(password), User.Role.ADMIN);
        admin.enabled = true;
        users.save(admin);
        System.out.println("[BootstrapAdminRunner] Admin user created successfully.");
    }
}
