package com.stayvida.backend.dto;

import java.util.List;

public class RegisterRoom {
    private int hotelId;
    private String roomType;
    private List<String> features; // JSON
    private List<String> images;   // JSON
    private int price;
    private int maxAdults;
    private int maxChildren;
    private int bedCount;

    // Getters & setters
    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getMaxAdults() { return maxAdults; }
    public void setMaxAdults(int maxAdults) { this.maxAdults = maxAdults; }

    public int getMaxChildren() { return maxChildren; }
    public void setMaxChildren(int maxChildren) { this.maxChildren = maxChildren; }

    public int getBedCount() { return bedCount; }
    public void setBedCount(int bedCount) { this.bedCount = bedCount; }
}
