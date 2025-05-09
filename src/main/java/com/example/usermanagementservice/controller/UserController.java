package com.example.usermanagementservice.controller;

import com.example.usermanagementservice.dto.AuthRequest;
import com.example.usermanagementservice.dto.AuthResponse;
import com.example.usermanagementservice.model.Token;
import com.example.usermanagementservice.model.User;
import com.example.usermanagementservice.repository.TokenRepository;
import com.example.usermanagementservice.repository.UserRepository;
import com.example.usermanagementservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private AuthenticationManager authManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;
    @Autowired private TokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public String register(@RequestBody AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return "User already exists";
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(request.getRoles());

        userRepository.save(user);
        return "User registered successfully";
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        final String token = jwtUtil.generateToken(user.getUsername(), user.getRoles());
        Token dbToken = new Token();
        dbToken.setUsername(user.getUsername());
        dbToken.setToken(token);
        dbToken.setCreatedAt(System.currentTimeMillis());
        tokenRepository.save(dbToken);
        return new AuthResponse(token);
    }
    @GetMapping("/validate")
    public ResponseEntity<Set<String>> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String token = authHeader.substring(7);
        boolean valid = jwtUtil.validateJwtToken(token);

        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = jwtUtil.getUsernameFromJwtToken(token);
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Set<String> roles = userOpt.get().getRoles();
        return ResponseEntity.ok(roles); // ✅ Return as JSON array
    }
}
