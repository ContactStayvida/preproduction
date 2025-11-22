package com.stayvida.backend.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Register {

    private String hotel_ID;
    private int owner_ID;
    private String name;
    private String type;
    private String destination;
    private Double rating;

    @JsonProperty("isForEvent")
    private boolean isForEvent;

    private String description;
    private String phone_NO;

    private List<String> tags;
    private List<String> amenities;

    private String images;
    private String longitude;
    private String latitude;

    // Removed from request:
    // private String status;         
    // private boolean onArrivalPayment;
    // private String remark;

    // Getters / Setters
    public String getHotel_ID() { return hotel_ID; }
    public void setHotel_ID(String hotel_ID) { this.hotel_ID = hotel_ID; }

    public int getOwner_ID() { return owner_ID; }
    public void setOwner_ID(int owner_ID) { this.owner_ID = owner_ID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public boolean isForEvent() { return isForEvent; }

    @JsonProperty("isForEvent")
    public void setForEvent(boolean forEvent) { this.isForEvent = forEvent; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhone_NO() { return phone_NO; }
    public void setPhone_NO(String phone_NO) { this.phone_NO = phone_NO; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }
}
