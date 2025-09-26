package com.stayvida.backend.model;

public class Register {
    private String hotel;
    private String location;
    private int maxAdults;
    private int maxChildren;
    private int maxRoom;
    private String description;

    // Getters & Setters
    public String getHotel() {
        return hotel;
    }
    public void setHotel(String hotel) {
        this.hotel = hotel;
    }

    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }

    public int getMaxAdults() {
        return maxAdults;
    }
    public void setMaxAdults(int maxAdults) {
        this.maxAdults = maxAdults;
    }

    public int getMaxChildren() {
        return maxChildren;
    }
    public void setMaxChildren(int maxChildren) {
        this.maxChildren = maxChildren;
    }

    public int getMaxRoom() {
        return maxRoom;
    }
    public void setMaxRoom(int maxRoom) {
        this.maxRoom = maxRoom;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}
