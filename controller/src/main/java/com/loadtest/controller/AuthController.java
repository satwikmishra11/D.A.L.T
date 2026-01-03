package com.loadtest.controller;

import com.loadtest.dto.AuthResponse;
import com.loadtest.model.User;
import com.loadtest.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody User request) {
        String token = authService.authenticate(
                request.getUsername(),
                request.getPassword()
        );
        return new AuthResponse(token);
    }
}
