package com.loadtest.service;

import com.loadtest.model.User;
import com.loadtest.repository.UserRepository;
import com.loadtest.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    public String authenticate(String username, String password) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid credentials");
        }

        return tokenProvider.generateToken(
                user.getId().toString(),
                user.getRole()
        );
    }
}
