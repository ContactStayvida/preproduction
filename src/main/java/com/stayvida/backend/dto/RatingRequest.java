package com.stayvida.backend.dto;

import java.time.LocalDateTime;

public class RatingRequest {
  private String bookingId;
  private String hotelId;
  private Long userId;
  private Double ratingValue;
  private String comment;
  private LocalDateTime rated_at;

  // Getters and setters
  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public String getHotelId() {
    return hotelId;
  }

  public void setHotelId(String hotelId) {
    this.hotelId = hotelId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Double getRatingValue() {
    return ratingValue;
  }

  public void setRatingValue(Double ratingValue) {
    this.ratingValue = ratingValue;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public LocalDateTime getRated_at() {
    return rated_at;
  }

  public void setRated_at(LocalDateTime rated_at) {
    this.rated_at = rated_at;
  }
}
