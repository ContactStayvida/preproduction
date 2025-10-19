package com.stayvida.backend.dto;

import java.util.List;

public class RoomDTO {
    private String roomId;
    private int hotelId;
    private String type;
    private double price;
    private int adultsMax;
    private int childrenMax;
    private int bedCount;
    private List<String> features;
    private List<String> roomImages;

    public RoomDTO(String roomId, int hotelId, String type, double price,
                   int adultsMax, int childrenMax, int bedCount) {
        this.roomId = roomId;
        this.hotelId = hotelId;
        this.type = type;
        this.price = price;
        this.adultsMax = adultsMax;
        this.childrenMax = childrenMax;
        this.bedCount = bedCount;
    }

    // Getters & Setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getAdultsMax() { return adultsMax; }
    public void setAdultsMax(int adultsMax) { this.adultsMax = adultsMax; }

    public int getChildrenMax() { return childrenMax; }
    public void setChildrenMax(int childrenMax) { this.childrenMax = childrenMax; }

    public int getBedCount() { return bedCount; }
    public void setBedCount(int bedCount) { this.bedCount = bedCount; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public List<String> getRoomImages() { return roomImages; }
    public void setRoomImages(List<String> roomImages) { this.roomImages = roomImages; }
}
