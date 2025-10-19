package com.stayvida.backend.dto;

public class HotelVerificationUpdate {
    private int hotelId;
    private String status; // "Rejected" or "Verified"
    private String remark; // new field

    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
