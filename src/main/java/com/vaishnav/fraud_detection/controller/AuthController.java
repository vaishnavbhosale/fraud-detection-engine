package com.vaishnav.fraud_detection.controller;

import com.vaishnav.fraud_detection.model.LoginRequest;
import com.vaishnav.fraud_detection.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        if (request.getUsername().equals(adminUsername)
                && request.getPassword().equals(adminPassword)) {

            String token = jwtUtil.generateToken(request.getUsername());

            return ResponseEntity.ok(Map.of("token", token));
        }

        return ResponseEntity.status(401)
                .body(Map.of("error", "Invalid credentials"));
    }
}