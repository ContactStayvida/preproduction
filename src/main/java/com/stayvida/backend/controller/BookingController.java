package com.stayvida.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.HashMap;
import java.util.Map;

// import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import com.stayvida.backend.service.JwtUtil;

import com.stayvida.backend.dto.BookingRequest;
import com.stayvida.backend.dto.BookingResponse;
import com.stayvida.backend.dto.LockRoomRequest;
import com.stayvida.backend.dto.LockRoomResponse;
import com.stayvida.backend.dto.ValidateCodeRequest;
import com.stayvida.backend.service.BookingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;
    private final JwtUtil jwtUtil;

    public BookingController(BookingService bookingService, JwtUtil jwtUtil) {
        this.bookingService = bookingService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/lock-room")
    public ResponseEntity<LockRoomResponse> lockRoom(@RequestBody LockRoomRequest request) {

        // Integer ownerId = (int) SecurityContextHolder
        // .getContext()
        // .getAuthentic ation()
        // .getPrincipal();

        LockRoomResponse response = bookingService.lockRoom(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody BookingRequest request) {

        Integer userId = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        BookingResponse response = bookingService.createBooking(userId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate-code")
    public ResponseEntity<Map<String, Boolean>> validateCode(@RequestParam String Code) {
        Boolean isValid = bookingService.validateCode(Code);
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);

        return ResponseEntity.ok(response);
    }

}