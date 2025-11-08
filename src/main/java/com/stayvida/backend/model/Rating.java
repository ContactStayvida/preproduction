 package com.stayvida.backend.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer rating_ID;

    private Integer user_ID;
    private Integer hotel_ID;
    private String booking_ID;

    private Double rating_Value;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime rated_at;

    // Getters & Setters
    public Integer getRating_ID() { return rating_ID; }
    public void setRating_ID(Integer rating_ID) { this.rating_ID = rating_ID; }

    public Integer getUser_ID() { return user_ID; }
    public void setUser_ID(Integer user_ID) { this.user_ID = user_ID; }

    public Integer getHotel_ID() { return hotel_ID; }
    public void setHotel_ID(Integer hotel_ID) { this.hotel_ID = hotel_ID; }

    public String getBooking_ID() { return booking_ID; }
    public void setBooking_ID(String booking_ID) { this.booking_ID = booking_ID; }

    public Double getRating_Value() { return rating_Value; }
    public void setRating_Value(Double rating_Value) { this.rating_Value = rating_Value; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getRated_at() { return rated_at; }
    public void setRated_at(LocalDateTime rated_at) { this.rated_at = rated_at; }
} 
