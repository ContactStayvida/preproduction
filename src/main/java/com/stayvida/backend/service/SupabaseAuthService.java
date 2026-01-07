package com.stayvida.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.stayvida.backend.dto.AuthRequest;
import java.util.Map;

@Service
public class SupabaseAuthService {

    @Value("${supabase.auth.url}")
    private String supabaseAuthUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 🔹 Signup user
    public ResponseEntity<?> signUp(AuthRequest request) {
        try {
            String authUrl = supabaseAuthUrl + "/signup";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseKey);

            Map<String, Object> body = Map.of(
                    "email", request.getEmail(),
                    "password", request.getPassword(),
                    "data", Map.of( // 👈 user_metadata
                            "display_name", request.getAdminName(),
                            "role", request.getRole()));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(authUrl, HttpMethod.POST, entity, String.class);

            return ResponseEntity.status(response.getStatusCode())
                    .body(Map.of(
                            "message", "User registered successfully using Supabase Auth"));

        } catch (Exception ex) {
            String message = ex.getMessage();

            if (message != null && message.contains("user_already_exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "User already exists"));
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Supabase signup failed",
                            "message", message));
        }
    }

    // 🔹 Login user
    public ResponseEntity<?> signIn(AuthRequest request) {
        String url = supabaseAuthUrl + "/token?grant_type=password";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseKey);

        Map<String, Object> body = Map.of(
                "email", request.getEmail(),
                "password", request.getPassword());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
    }
}
