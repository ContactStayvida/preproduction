package com.stayvida.backend.model;

public class Amenity {
    private int amenity_id;
    private String name;
    private String status;

    public int getAmenity_id() { return amenity_id; }
    public void setAmenity_id(int amenity_id) { this.amenity_id = amenity_id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
