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
        // Step 1: Create user in Supabase Auth
        String authUrl = supabaseAuthUrl + "/signup";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseKey);

        Map<String, Object> body = Map.of(
                "email", request.getEmail(),
                "password", request.getPassword()
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // 🔸 Use String.class instead of Map.class (some responses are empty)
        ResponseEntity<String> response = restTemplate.exchange(authUrl, HttpMethod.POST, entity, String.class);

        // Step 2: Insert into your admins table using Supabase REST API
        if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 201) {

            String insertUrl = "https://vpdeibjihiottdvuqsms.supabase.co/rest/v1/admins";

            HttpHeaders insertHeaders = new HttpHeaders();
            insertHeaders.setContentType(MediaType.APPLICATION_JSON);
            insertHeaders.set("apikey", supabaseKey);
            insertHeaders.set("Authorization", "Bearer " + supabaseKey);
            insertHeaders.set("Prefer", "return=minimal"); // no response body

            Map<String, Object> adminRecord = Map.of(
                    "email", request.getEmail(),
                    "admin_name", request.getAdminName(),
                    "role", request.getRole()
            );

            HttpEntity<Map<String, Object>> insertEntity = new HttpEntity<>(adminRecord, insertHeaders);

            // 🔸 No need to parse JSON here, use String.class safely
            restTemplate.exchange(insertUrl, HttpMethod.POST, insertEntity, String.class);
        }

        return ResponseEntity.status(response.getStatusCode())
                .body(Map.of("message", "User registered successfully in Supabase Auth and admins table"));

    } catch (Exception ex) {
        String message = ex.getMessage();

        // Handle “user already exists” gracefully
        if (message != null && message.contains("user_already_exists")) {
            try {
                // Try inserting into admin table even if already in auth
                String insertUrl = "https://vpdeibjihiottdvuqsms.supabase.co/rest/v1/admins";
                HttpHeaders insertHeaders = new HttpHeaders();
                insertHeaders.setContentType(MediaType.APPLICATION_JSON);
                insertHeaders.set("apikey", supabaseKey);
                insertHeaders.set("Authorization", "Bearer " + supabaseKey);
                insertHeaders.set("Prefer", "return=minimal");

                Map<String, Object> adminRecord = Map.of(
                        "email", request.getEmail(),
                        "admin_name", request.getAdminName(),
                        "role", request.getRole()
                );

                HttpEntity<Map<String, Object>> insertEntity = new HttpEntity<>(adminRecord, insertHeaders);
                restTemplate.exchange(insertUrl, HttpMethod.POST, insertEntity, String.class);
            } catch (Exception ignored) {}

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "User already exists in Supabase Auth but ensured in admins table"));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Supabase signup failed", "message", message));
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
                "password", request.getPassword()
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
    }
}
