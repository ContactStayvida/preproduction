// src/main/java/com/stayvida/backend/dto/RegisterRoom.java
package com.stayvida.backend.dto;

public class RegisterRoom {
    private int hotelId;
    private int adultsMax;
    private int childrenMax;
    private String type;
    private int price;

    // Getters and setters
    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }

    public int getAdultsMax() { return adultsMax; }
    public void setAdultsMax(int adultsMax) { this.adultsMax = adultsMax; }

    public int getChildrenMax() { return childrenMax; }
    public void setChildrenMax(int childrenMax) { this.childrenMax = childrenMax; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
}
