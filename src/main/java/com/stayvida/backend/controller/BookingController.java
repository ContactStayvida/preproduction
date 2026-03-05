package com.stayvida.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import com.stayvida.backend.dto.BookingRequest;
import com.stayvida.backend.dto.BookingResponse;
import com.stayvida.backend.dto.LockRoomRequest;
import com.stayvida.backend.dto.LockRoomResponse;
import com.stayvida.backend.service.BookingService;

@RestController
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/lock-room")
    public ResponseEntity<LockRoomResponse> lockRoom(
            @RequestBody LockRoomRequest request) {

        // Integer ownerId = (int) SecurityContextHolder
        // .getContext()
        // .getAuthentication()
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
}
