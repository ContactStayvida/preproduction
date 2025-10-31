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
                                        Authentication authentication) throws IOException, ServletException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();
        String firstName = oidcUser.getGivenName();
        String lastName = oidcUser.getFamilyName();
        String username = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");

        // 🧾 Create or update user
        User user = new User();
        user.setEmail(email);
        user.setUsername(username.trim());
        user.setPassword("GOOGLE_LOGIN");
        user.setRole("user"); // only used on first insert
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // ✅ Save or update user (auto handles duplicates)
        userRepository.saveOrUpdate(user);

        // 🧾 Generate JWT token
        String token = jwtUtil.generateToken(email);

        // 🎯 Return JSON response instead of redirect
        String jsonResponse = String.format(
            "{\"success\":true,\"token\":\"%s\",\"user id\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\"}",
            token,user.getuserID() ,user.getUsername(), user.getEmail(), user.getRole()
        );

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse);
    }
}
