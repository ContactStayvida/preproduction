package com.stayvida.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class GoogleLoginController {

    @GetMapping("/me")
    @ResponseBody
    public Map<String, Object> me(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) return Map.of("authenticated", false);
        return Map.of(
            "authenticated", true,
            "name", oidcUser.getFullName(),
            "email", oidcUser.getEmail()
            // "claims", oidcUser.getClaims()
        );
    }

        @GetMapping("/login")
    public String login(@AuthenticationPrincipal OidcUser principal) {
        
        if (principal != null) {
            // Already logged in → redirect to dashboard
            return "redirect:https://www.youtube.com"; //switch this to homepage later
        }
        // Not logged in → redirect to Google OAuth2 login
        return "redirect:/oauth2/authorization/google";
    }


    @GetMapping("/logout-success")
@ResponseBody  // optional if you just want simple text output without a template
public String logoutSuccess(
        @AuthenticationPrincipal OidcUser principal,
        @RequestParam(value = "logout", required = false) String logout
) {

        // User just logged out
        return "You have successfully logged out!" + "Please log in via Google OAuth2: <a href='/oauth2/authorization/google'>Login</a>";
}


    








}


