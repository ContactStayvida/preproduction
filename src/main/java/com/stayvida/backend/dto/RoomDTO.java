package com.stayvida.backend.dto;

public class RoomDTO {
      private int hotelId;
    private int roomId;
    private int adultsMax;
    private int childrenMax;
    private String type;
    private double price;

    // Constructor
    public RoomDTO(int hotelId, int roomId, int adultsMax, int childrenMax, String type, double price) {
        this.hotelId = hotelId;
        this.roomId = roomId;
        this.adultsMax = adultsMax;
        this.childrenMax = childrenMax;
        this.type = type;
        this.price = price;
    }

    // Getters & Setters
    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public int getAdultsMax() { return adultsMax; }
    public void setAdultsMax(int adultsMax) { this.adultsMax = adultsMax; }

    public int getChildrenMax() { return childrenMax; }
    public void setChildrenMax(int childrenMax) { this.childrenMax = childrenMax; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
}
