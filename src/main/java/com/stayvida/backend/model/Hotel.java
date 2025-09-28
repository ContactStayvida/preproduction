package com.stayvida.backend.model;

import java.util.List;

// import org.springframework.security.access.method.P;

public class Hotel {
    private int id;
    private String hotel;
    private String location;
    private int price;
    private int adult;
    private int children;
    private boolean availability; // <- add this
    // private byte[] image; // 👈 Store raw image as byte array
    private String imagePath;// New field for image path
    private double rating; // New field for rating
    private String description; // New field for description

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getHotel() { return hotel; }
    public void setHotel(String hotel) { this.hotel = hotel; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    // public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getAdults() { return adult; }
    public void setAdult(int adult) { this.adult = adult; }

    public int getchildren() { return children; }
    public void setchildren(int children) { this.children = children; }


    public boolean isAvailability() { return availability; }
    public void setAvailability(boolean availability) { this.availability = availability; }

    // public byte[] getImage() { return image; }
    // public void setImage(byte[] image) { this.image = image; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public double getRating() {return rating;}
    public void setRating(double rating) { this.rating = rating; }
    public String getdescription() { return description; }
    public void setdescription(String description) { this.description = description; }
    
    private List<String> amenities; // New field for amenities
    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
}