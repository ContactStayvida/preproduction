package com.stayvida.backend.security;

import com.stayvida.backend.model.User;
import com.stayvida.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public CustomOAuth2SuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        // Save user if not exists
        User existingUser = userRepository.findByEmail(email);
        if (existingUser == null) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(name != null ? name : email);
            newUser.setPassword("OAUTH_USER"); // placeholder
            userRepository.save(newUser);
        }

        // Redirect after login
        response.sendRedirect("https://www.instagram.com/tanaymithari21");//switch this to home page later
    }
}
