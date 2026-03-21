package com.stayvida.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BookingEmailDTO {

    private String bookingId;
    private String hotelName;

    private LocalDate checkIn;
    private LocalDate checkOut;

    private int roomNo;

    private BigDecimal totalAmount;
    private BigDecimal platformFee;
    private BigDecimal taxAmount;

    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;

    private String paymentStatus;
    private String paymentType;

    private String guestName;
    private String phone;

    public BookingEmailDTO() {
    }

    public BookingEmailDTO(String bookingId, String hotelName, LocalDate checkIn, LocalDate checkOut, int roomNo,
            BigDecimal totalAmount, BigDecimal platformFee, BigDecimal taxAmount, BigDecimal paidAmount,
            BigDecimal pendingAmount, String paymentStatus, String paymentType, String guestName, String phone) {
        this.bookingId = bookingId;
        this.hotelName = hotelName;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.roomNo = roomNo;
        this.totalAmount = totalAmount;
        this.platformFee = platformFee;
        this.taxAmount = taxAmount;
        this.paidAmount = paidAmount;
        this.pendingAmount = pendingAmount;
        this.paymentStatus = paymentStatus;
        this.paymentType = paymentType;
        this.guestName = guestName;
        this.phone = phone;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public int getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(int roomNo) {
        this.roomNo = roomNo;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(BigDecimal pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}