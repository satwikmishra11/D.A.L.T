package com.loadtest.config;

import com.loadtest.model.User;
import com.loadtest.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.findByUsername("demo@loadtest.pro").isPresent()) {
            User demoUser = User.builder()
                    .username("demo@loadtest.pro")
                    .password(passwordEncoder.encode("demo123"))
                    .role(User.Role.ADMIN)
                    .organizationId("demo-org")
                    .build();
            userRepository.save(demoUser);
        }
    }
}
