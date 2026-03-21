package com.stayvida.backend.dto;

public class HotelUpdateRequest {

    private String name;
    private String type;
    private String destination;
    private Boolean isForEvent;
    private String description;
    private String phone_NO;
    private String country_code;
    private String tags;
    private String amenities;
    private String longitude;
    private String latitude;

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

    public Boolean getIsForEvent() {
        return isForEvent;
    }

    public void setIsForEvent(Boolean isForEvent) {
        this.isForEvent = isForEvent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhone_NO() {
        return phone_NO;
    }

    public void setPhone_NO(String phone_NO) {
        this.phone_NO = phone_NO;
    }

    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

}