package com.stayvida.backend.service;

import com.stayvida.backend.model.Feature;
import com.stayvida.backend.model.Amenity;
import com.stayvida.backend.model.Tag;
import com.stayvida.backend.repository.LookupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LookupService {

    @Autowired
    private LookupRepository repo;

    // public void addFeature(String name) { repo.addFeature(name); }

    // public void addAmenity(String name) { repo.addAmenity(name); }

    // public void addTag(String name) { repo.addTag(name); }

    public Map<String, List<String>> addFeatures(List<String> names) {
        return repo.addFeatures(names);
    }

    public Map<String, List<String>> addAmenities(List<String> names) {
        return repo.addAmenities(names);
    }

    public Map<String, List<String>> addTags(List<String> names) {
        return repo.addTags(names);
    }

    public List<Feature> getFeatures() {
        return repo.getFeatures();
    }

    public List<Amenity> getAmenities() {
        return repo.getAmenities();
    }

    public List<Tag> getTags() {
        return repo.getTags();
    }
}
