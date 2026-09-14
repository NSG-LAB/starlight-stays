package com.starlight.userservice.controller;
import com.starlight.userservice.config.JwtUtil;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/users")
public class AuthController {
    private final JwtUtil jwtUtil;
    public AuthController(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        if("admin".equals(username) && "password".equals(password)) { return jwtUtil.generateToken(username); }
        throw new RuntimeException("Invalid Credentials");
    }

    @GetMapping("/profile")
    public java.util.Map<String, Object> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String username = "admin";
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                username = jwtUtil.extractUsername(authHeader.substring(7));
            } catch (Exception ignored) {}
        }
        return java.util.Map.of(
            "username", username,
            "role", "ADMIN",
            "status", "ACTIVE",
            "email", username + "@starlightstays.luxury"
        );
    }
}
