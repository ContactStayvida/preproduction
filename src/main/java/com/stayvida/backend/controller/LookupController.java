package com.stayvida.backend.controller;

import com.stayvida.backend.model.Feature;
import com.stayvida.backend.model.Amenity;
import com.stayvida.backend.model.Tag;
import com.stayvida.backend.service.LookupService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lookup")
public class LookupController {

    @Autowired
    private LookupService service;

    // // ---------- ADD ----------
    // @PostMapping("/feature")
    // public String addFeature(@RequestParam String name) {
    // service.addFeature(name);
    // return "Feature added successfully";
    // }

    // @PostMapping("/amenity")
    // public String addAmenity(@RequestParam String name) {
    // service.addAmenity(name);
    // return "Amenity added successfully";
    // }

    // @PostMapping("/tag")
    // public String addTag(@RequestParam String name) {
    // service.addTag(name);
    // return "Tag added successfully";
    // }

    // ---------- GET ALL ----------
    @GetMapping("/features")
    public List<Feature> getFeatures() {
        return service.getFeatures();
    }

    @GetMapping("/amenities")
    public List<Amenity> getAmenities() {
        return service.getAmenities();
    }

    @GetMapping("/tags")
    public List<Tag> getTags() {
        return service.getTags();
    }
}
