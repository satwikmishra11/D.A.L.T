package com.loadtest.controller;

import com.loadtest.dto.AuthResponse;
import com.loadtest.model.User;
import com.loadtest.service.AuthService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API for user authentication and token generation")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates a user and returns a JWT token")
    @RateLimiter(name = "auth")
    public AuthResponse login(@RequestBody User request) {
        log.info("Login attempt for username: {}", request.getUsername());
        String token = authService.authenticate(
                request.getUsername(),
                request.getPassword()
        );
        log.info("Login successful for username: {}", request.getUsername());
        return new AuthResponse(token);
    }
}
