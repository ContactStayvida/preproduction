package com.stayvida.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stayvida.backend.service.LocationService;

@RestController
@RequestMapping("/api/locations")
@CrossOrigin
public class LocationController {

    @Autowired
    private LocationService locationService;

    @GetMapping
    public ResponseEntity<List<String>> search(
            @RequestParam String q) {

        List<String> destinations = locationService.getDestinations(q);
        return ResponseEntity.ok(destinations);
    }
}