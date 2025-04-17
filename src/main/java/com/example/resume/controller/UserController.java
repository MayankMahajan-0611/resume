package com.example.resume.controller;

import com.example.resume.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;



import com.example.resume.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
//@CrossOrigin("*")
@RestController
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> payload) {
        try {
            String username = payload.get("username");
            String password = payload.get("password");

            if (username == null || password == null) {
                return ResponseEntity.badRequest().body("Username and password are required.");
            }

            userService.registerUser(username, password);
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        try {
            String username = payload.get("username");
            String password = payload.get("password");

            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required."));
            }

            Long userId = userService.authenticateUser(username, password);
            if (userId != null) {
            Map<String, Object> response = Map.of(
                    "userId", userId,
                    "username", username
            );
            return ResponseEntity.ok(response);

            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "An error occurred during login."));
        }
    }
}