package com.stayvida.backend.dto;

import java.util.List;
// import java.util.Map;

public class HotelDTO {
    private int hotelId;
    private String name;
    private String description;
    private List<String> amenities;
    private double rating;
    private String destination;
    private boolean onArrivalPayment;
    private boolean isForEvent;
    private List<String> images;
    private List<String> tags;

    private List<RoomDTO> rooms; // List of all rooms for this hotel

    // Getters & Setters
    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public boolean isOnArrivalPayment() { return onArrivalPayment; }
    public void setOnArrivalPayment(boolean onArrivalPayment) { this.onArrivalPayment = onArrivalPayment; }

    public boolean isForEvent() { return isForEvent; }
    public void setForEvent(boolean forEvent) { isForEvent = forEvent; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<RoomDTO> getRooms() { return rooms; }
    public void setRooms(List<RoomDTO> rooms) { this.rooms = rooms; }
    
    public List<String> getAmenities() {return amenities;}
    public void setAmenities(List<String> amenities) {this.amenities = amenities;}

}
