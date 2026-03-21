package com.stayvida.backend.model;

import java.util.List;

public class Hotel {

    private String id; // hotel_ID
    // private int ownerId;
    private String name;
    private String type; // 'Hotel','Resort','Villa','Guest House'
    private String destination;
    private double rating;
    private boolean isForEvent;
    private double price;
    // private String description;
    // private String phoneNo;
    // private List<String> tags; // JSON array
    private String image; // single file name
    private List<String> amenities; // JSON array
    // private String longitude;
    // private String latitude;
    // private String status; // 'Pending','Verified','Rejected'
    // private boolean onArrivalPayment;
    // private String remark;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // public int getOwnerId() { return ownerId; }
    // public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public boolean isForEvent() {
        return isForEvent;
    }

    public void setForEvent(boolean forEvent) {
        isForEvent = forEvent;
    }

    // public String getDescription() { return description; }
    // public void setDescription(String description) { this.description =
    // description; }

    // public String getPhoneNo() { return phoneNo; }
    // public void setPhoneNo(String phoneNo) { this.phoneNo = phoneNo; }

    // public List<String> getTags() { return tags; }
    // public void setTags(List<String> tags) { this.tags = tags; }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    // public String getLongitude() { return longitude; }
    // public void setLongitude(String longitude) { this.longitude = longitude; }

    // public String getLatitude() { return latitude; }
    // public void setLatitude(String latitude) { this.latitude = latitude; }

    // public String getStatus() { return status; }
    // public void setStatus(String status) { this.status = status; }

    // public boolean isOnArrivalPayment() { return onArrivalPayment; }
    // public void setOnArrivalPayment(boolean onArrivalPayment) {
    // this.onArrivalPayment = onArrivalPayment; }

    // public String getRemark() { return remark; }
    // public void setRemark(String remark) { this.remark = remark; }
}
