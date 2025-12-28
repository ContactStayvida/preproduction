package com.stayvida.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

public class RoomDTO {
    private String roomId;
    private Integer room_NO;
    private int hotelId;
    private String type;
    private double price;
    private double platformCharges;
    private double taxPercent;
    private long stayDuration;
    private int adultsMax;
    private int childrenMax;
    private int bedCount;
    private List<String> features;
    private List<String> roomImages;

    @JsonPropertyOrder({ "roomId", "room_NO", "hotelId", "type", "price", "platformCharges",
            "taxPercent", "stayDuration", "adultsMax", "childrenMax", "bedCount", "features", "roomImages" })
    public RoomDTO(String roomId, Integer room_NO, int hotelId, String type, double price,
            double platformCharges, double taxPercent, long stayDuration, int adultsMax, int childrenMax,
            int bedCount) {
        this.roomId = roomId;
        this.room_NO = room_NO;
        this.hotelId = hotelId;
        this.type = type;
        this.price = price;
        this.platformCharges = platformCharges;
        this.taxPercent = taxPercent;
        this.adultsMax = adultsMax;
        this.childrenMax = childrenMax;
        this.bedCount = bedCount;
        this.stayDuration = stayDuration;

    }

    // Getters & Setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setRoom_NO(Integer room_NO) {
        this.room_NO = room_NO;
    }

    public Integer getRoom_NO() {
        return room_NO;
    }

    public int getHotelId() {
        return hotelId;
    }

    public void setHotelId(int hotelId) {
        this.hotelId = hotelId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPrice() {
        return price;
    }

    public double getPlatformCharges() {
        return platformCharges;
    }

    public void setPlatformCharges(double platformCharges) {
        this.platformCharges = platformCharges;
    }

    public double getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(double taxPercent) {
        this.taxPercent = taxPercent;
    }

    public long getStayDuration() {
        return stayDuration;
    }

    public void setStayDuration(long stayDuration) {
        this.stayDuration = stayDuration;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getAdultsMax() {
        return adultsMax;
    }

    public void setAdultsMax(int adultsMax) {
        this.adultsMax = adultsMax;
    }

    public int getChildrenMax() {
        return childrenMax;
    }

    public void setChildrenMax(int childrenMax) {
        this.childrenMax = childrenMax;
    }

    public int getBedCount() {
        return bedCount;
    }

    public void setBedCount(int bedCount) {
        this.bedCount = bedCount;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public List<String> getRoomImages() {
        return roomImages;
    }

    public void setRoomImages(List<String> roomImages) {
        this.roomImages = roomImages;
    }

}
