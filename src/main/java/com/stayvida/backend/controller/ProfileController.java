package com.stayvida.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stayvida.backend.model.Profile;
import com.stayvida.backend.security.ApiResponse;
import com.stayvida.backend.service.ProfileService;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @PostMapping("/{UserID}")
public ResponseEntity<?> createOrUpdate(
        @PathVariable Integer UserID,
        @RequestBody Profile profile) {

    profile.setUserID(UserID);
    Profile saved = profileService.saveProfile(profile);

    return ApiResponse.success(saved, "Profile updated successfully");
}

 @GetMapping("/{UserID}")
public ResponseEntity<?> get(@PathVariable Integer UserID) {

    Profile profile = profileService.getProfile(UserID);

    if (profile == null) {
        return ApiResponse.notFound("Profile not found for userID: " + UserID);
    }

    return ApiResponse.success(profile, "Profile fetched successfully");
}

}
