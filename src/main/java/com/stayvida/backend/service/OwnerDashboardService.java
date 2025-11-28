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

        // ===============================
        // HOTEL & ROOM WISE DATA
        // ===============================
        for (Map<String, Object> hotel : hotels) {

            int hotelId = (int) hotel.get("hotel_ID");
            String hotelName = (String) hotel.get("name");

            int hotelBookings = repo.getMonthlyBookingCount(hotelId);
            totalMonthlyBookings += hotelBookings;

            int hotelGuests = repo.getTotalGuestsForMonth(hotelId);
            totalGuests += hotelGuests;

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

            Map<String, Object> hotelData = new HashMap<>();
            hotelData.put("hotelId", hotelId);
            hotelData.put("hotelName", hotelName);
            hotelData.put("monthlyBookings", hotelBookings);
            hotelData.put("totalGuests", hotelGuests);
            hotelData.put("rooms", roomWiseList);

            hotelWiseList.add(hotelData);
        }


        // ===============================
        // CURRENT MONTH TOTAL METRICS
        // ===============================
        double totalRevenue = repo.getMonthlyRevenueForOwner(ownerId);


        // ===============================
        // LAST MONTH METRICS
        // ===============================
        double lastMonthRevenue = repo.getLastMonthRevenueForOwner(ownerId);
        int lastMonthBookings = repo.getLastMonthBookingCount(ownerId);
        int lastMonthGuests = repo.getLastMonthGuestCount(ownerId);


        // ===============================
        // COMPARISON PERCENTAGE
        // ===============================
        String revenueChange = calculateChange(totalRevenue, lastMonthRevenue);
        String bookingChange = calculateChange(totalMonthlyBookings, lastMonthBookings);
        String guestChange = calculateChange(totalGuests, lastMonthGuests);


        // ===============================
        // ROOMS OCCUPIED TODAY
        // ===============================
        int roomsOccupiedToday = repo.getRoomsOccupiedToday(ownerId);


        // ===============================
        // FINAL RESPONSE OBJECT
        // ===============================
        response.put("totalMonthlyBookings", totalMonthlyBookings);
        response.put("totalGuests", totalGuests);
        response.put("totalRevenue", totalRevenue);

        response.put("revenueDifference", revenueChange);
        response.put("bookingDifference", bookingChange);
        response.put("guestDifference", guestChange);

        response.put("roomsOccupied", roomsOccupiedToday);

        response.put("hotelWiseBookings", hotelWiseList);

        return response;
    }



    // ===============================
    // PERCENTAGE CHANGE CALCULATOR
    // ===============================
    private String calculateChange(double current, double last) {

        if (last == 0 && current == 0) return "0% same as last month";

        if (last == 0) return "+100% increased from last month"; // Edge case

        double change = ((current - last) / last) * 100.0;
        String percent = String.format("%.2f", Math.abs(change));

        return change >= 0
                ? "+" + percent + "% increased from last month"
                : "-" + percent + "% decreased from last month";
    }
}
