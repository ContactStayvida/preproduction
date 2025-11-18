package com.stayvida.backend.service;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stayvida.backend.repository.OwnerDashboardRepository;

@Service
public class OwnerDashboardService {

    @Autowired
    private OwnerDashboardRepository repo;

    public Map<String, Object> getMonthlyBookingsForOwner(int ownerId) {

        Map<String, Object> response = new HashMap<>();

        List<Map<String, Object>> hotels = repo.getOwnerVerifiedHotels(ownerId);

        List<Map<String, Object>> hotelWiseList = new ArrayList<>();
        int totalMonthlyBookings = 0;
        int totalGuests = 0;

        for (Map<String, Object> hotel : hotels) {
            int hotelId = (int) hotel.get("hotel_ID");
            String hotelName = (String) hotel.get("name");

            // Hotel monthly bookings
            int hotelBookings = repo.getMonthlyBookingCount(hotelId);
            totalMonthlyBookings += hotelBookings;

            // Total guests for this hotel
            int hotelGuests = repo.getTotalGuestsForMonth(hotelId); // you need to add this method
            totalGuests += hotelGuests;

            // Fetch rooms
            List<Map<String, Object>> rooms = repo.getRoomsByHotelId(hotelId);
            List<Map<String, Object>> roomWiseList = new ArrayList<>();

            for (Map<String, Object> room : rooms) {
                String roomId = (String) room.get("room_ID");
                String roomType = (String) room.get("room_Type");

                int roomBookings = repo.getMonthlyBookingCountForRoom(roomId);

                Map<String, Object> roomData = new HashMap<>();
                roomData.put("roomId", roomId);
                roomData.put("roomType", roomType);
                roomData.put("monthlyBookings", roomBookings);

                roomWiseList.add(roomData);
            }

            // Prepare hotel final data
            Map<String, Object> hotelData = new HashMap<>();
            hotelData.put("hotelId", hotelId);
            hotelData.put("hotelName", hotelName);
            hotelData.put("monthlyBookings", hotelBookings);
            hotelData.put("totalGuests", hotelGuests);
            hotelData.put("rooms", roomWiseList);

            hotelWiseList.add(hotelData);
        }

        // Total revenue for all hotels of this owner
        double totalRevenue = repo.getMonthlyRevenueForOwner(ownerId); // add method in repo

        response.put("totalMonthlyBookings", totalMonthlyBookings);
        response.put("totalGuests", totalGuests);
        response.put("totalRevenue", totalRevenue);
        response.put("hotelWiseBookings", hotelWiseList);

        return response;
    }
}
