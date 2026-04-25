package com.stayvida.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class welcome {

    @GetMapping("/")
    public String welcome() {
        return "Welcome to StayVida Backend";
    }

}
