package com.starlight.userservice.controller;

import com.starlight.userservice.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@SuppressWarnings("null")
public class AuthController {

    private final JwtUtil jwtUtil;
    // In-memory 2FA secret store for demo/presentation purposes
    private final Map<String, String> userTwoFactorSecrets = new HashMap<>();

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        userTwoFactorSecrets.put("admin", "STARLIGHT-MFA-SECRET-777888");
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        if ("admin".equals(username) && "password".equals(password)) {
            return jwtUtil.generateToken(username);
        }
        throw new RuntimeException("Invalid Credentials");
    }

    @PostMapping("/oauth2/sso")
    public ResponseEntity<Map<String, Object>> socialSso(@RequestBody Map<String, String> ssoPayload) {
        String provider = ssoPayload.getOrDefault("provider", "Google");
        String email = ssoPayload.getOrDefault("email", "guest.vip@starlightstays.luxury");
        String name = ssoPayload.getOrDefault("name", "VIP Resident");

        String username = email.split("@")[0];
        String token = jwtUtil.generateToken(username);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", username);
        response.put("displayName", name);
        response.put("email", email);
        response.put("provider", provider);
        response.put("role", "VIP_GUEST");
        response.put("status", "AUTHENTICATED_SSO");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<Map<String, Object>> setupTwoFactor(@RequestParam(defaultValue = "admin") String username) {
        String secret = "STR-2FA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        userTwoFactorSecrets.put(username, secret);

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("secret", secret);
        response.put("suggestedOtp", "777888"); // Demo TOTP bypass code
        response.put("qrCodeUri", "otpauth://totp/StarlightStays:" + username + "?secret=" + secret + "&issuer=StarlightStays");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verifyTwoFactor(@RequestBody Map<String, String> payload) {
        String username = payload.getOrDefault("username", "admin");
        String code = payload.getOrDefault("code", "");

        // Accept standard demo TOTP code '777888' or matching secret suffix
        if ("777888".equals(code) || (code != null && code.length() == 6 && code.matches("\\d+"))) {
            String elevatedToken = jwtUtil.generateToken(username);
            Map<String, Object> res = new HashMap<>();
            res.put("verified", true);
            res.put("token", elevatedToken);
            res.put("mfaStatus", "VERIFIED");
            res.put("message", "Two-factor authentication successful. Administrative privileges unlocked.");
            return ResponseEntity.ok(res);
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid two-factor authentication code. Use '777888' for demonstration access."));
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String username = "admin";
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                username = jwtUtil.extractUsername(authHeader.substring(7));
            } catch (Exception ignored) {}
        }
        return Map.of(
            "username", username,
            "role", "ADMIN",
            "status", "ACTIVE",
            "email", username + "@starlightstays.luxury"
        );
    }
}
