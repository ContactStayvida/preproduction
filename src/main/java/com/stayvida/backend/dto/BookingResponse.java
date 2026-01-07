package com.stayvida.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingResponse {

    private String bookingId;
    private String bookingStatus;
    private String paymentStatus;
    private BigDecimal roomPrice;
    private BigDecimal platformCharges;
    // private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private LocalDateTime createdAt;
    private String checkIn;
    private String checkOut;
    private long duration;

    // getters & setters
    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getRoomPrice() {
        return roomPrice;
    }

    public void setRoomPrice(BigDecimal roomPrice) {
        this.roomPrice = roomPrice;
    }

    public BigDecimal getPlatformCharges() {
        return platformCharges;
    }

    public void setPlatformCharges(BigDecimal platformCharges) {
        this.platformCharges = platformCharges;
    }

    // public BigDecimal getTaxRate() {
    // return taxRate;
    // }

    // public void setTaxRate(BigDecimal taxRate) {
    // this.taxRate = taxRate;
    // }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(String checkIn) {
        this.checkIn = checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(String checkOut) {
        this.checkOut = checkOut;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
