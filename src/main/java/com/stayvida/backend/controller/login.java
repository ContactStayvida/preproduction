package com.stayvida.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
public class login {

    @GetMapping("/home")
    public Map<String, Object> home(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> userDetails = new HashMap<>();
        if (principal != null) {
            userDetails.put("name", principal.getAttribute("name"));
            userDetails.put("email", principal.getAttribute("email"));
        } else {
            userDetails.put("error", "No OAuth2User principal found. Login required.");
        }
        return userDetails;
    }
}
