package com.stayvida.backend.service;

import org.springframework.stereotype.Service;

import com.stayvida.backend.dto.BookingRequest;
import com.stayvida.backend.dto.BookingResponse;

@Service
public class BookingFlowService {

    private final UserService userService;
    private final BookingService bookingService;

    public BookingFlowService(UserService userService,
            BookingService bookingService) {
        this.userService = userService;
        this.bookingService = bookingService;
    }

    public BookingResponse initiateBooking(BookingRequest request) {

        String email = request.getEmail();

        // Ensure user exists
        userService.findOrCreateUser(email);

        // Continue booking using email
        return bookingService.createBookingod(email, request);
    }
}