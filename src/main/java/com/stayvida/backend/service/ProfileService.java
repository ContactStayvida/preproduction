package com.stayvida.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stayvida.backend.model.Profile;
import com.stayvida.backend.repository.ProfileRepository;

@Service
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    public Profile saveProfile(Profile profile) {
        // createOrUpdate already returns joined data (profile + email + role)
        return profileRepository.createOrUpdate(profile);
    }

    public Profile getProfile(Integer userId) {
        return profileRepository.getProfile(userId);
    }
}
