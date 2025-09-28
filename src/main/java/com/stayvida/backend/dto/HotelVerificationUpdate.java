package com.stayvida.backend.dto;

public class HotelVerificationUpdate {
    private int hotelId;
    private String status; // "Rejected" or "Verified"

    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
