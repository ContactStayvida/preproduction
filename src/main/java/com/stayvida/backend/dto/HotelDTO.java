package com.stayvida.backend.dto;

import java.util.List;
// import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class HotelDTO {
    private String hotelId;
    private String name;
    private String description;
    private List<String> amenities;
    private double rating;
    private String destination;
    private boolean onArrivalPayment;
    private boolean isForEvent;
    private List<String> images;
    private List<String> tags;
    private String countryCode; // ✅ ADD THIS
    private String phoneNo;
    private List<GroupedRoomDTO> rooms; // List of all rooms for this hotel

    // Getters & Setters
    public String getHotelId() {
        return hotelId;
    }

    // public List<RoomDTO> getRawRooms() {
    // return rawRooms;
    // }

    // public void setRawRooms(List<RoomDTO> rawRooms) {
    // this.rawRooms = rawRooms;
    // }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isOnArrivalPayment() {
        return onArrivalPayment;
    }

    public void setOnArrivalPayment(boolean onArrivalPayment) {
        this.onArrivalPayment = onArrivalPayment;
    }

    public boolean isForEvent() {
        return isForEvent;
    }

    public void setForEvent(boolean forEvent) {
        isForEvent = forEvent;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<GroupedRoomDTO> getRooms() {
        return rooms;
    }

    public void setRooms(List<GroupedRoomDTO> rooms) {
        this.rooms = rooms;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

}
