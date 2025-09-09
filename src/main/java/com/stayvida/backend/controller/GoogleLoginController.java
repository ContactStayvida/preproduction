package com.stayvida.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GoogleLoginController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) return Map.of("authenticated", false);
        return Map.of(
            "authenticated", true,
            "name", oidcUser.getFullName(),
            "email", oidcUser.getEmail()
            // "claims", oidcUser.getClaims()
        );
    }
}