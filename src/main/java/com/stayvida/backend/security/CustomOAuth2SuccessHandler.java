package com.stayvida.backend.security;

import com.stayvida.backend.model.User;
import com.stayvida.backend.repository.UserRepository;
import com.stayvida.backend.service.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public CustomOAuth2SuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();

        // 🔍 Check if user exists in DB
        User existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser != null) {
            // ✅ Existing user → use DB values
            user = existingUser;
            System.out.println("Existing user login: " + email);
        } else {
            // 🆕 New user → create and insert
            user = new User();
            user.setEmail(email);
            user.setPassword("GOOGLE_LOGIN");
            user.setRole("user");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.saveOrUpdate(user);
        }

        // 🧾 Generate JWT token
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getuserID(),
                user.getRole());

        // 🎯 Return JSON response
        String jsonResponse = String.format(
                "{\"success\":true,\"token\":\"%s\",\"userId\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}",
                token, user.getuserID(), user.getEmail(), user.getRole());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse);
    }
}
