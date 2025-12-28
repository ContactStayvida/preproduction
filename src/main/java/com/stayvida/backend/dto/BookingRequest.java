package com.stayvida.backend.dto;

import java.math.BigDecimal;

public class BookingRequest {

    private String lockRoomId; // room_id from lock
    private int adults;
    private int children;
    private String paymentType; // Partial / Advance / OnArrival
    private String name;
    private String countryCode;
    private String phoneNo;
    private String checkIn;
    private String checkOut;
    private BigDecimal taxAmount;

    // getters & setters
    public String getLockRoomId() {
        return lockRoomId;
    }

    public void setLockRoomId(String lockRoomId) {
        this.lockRoomId = lockRoomId;
    }

    public int getAdults() {
        return adults;
    }

    public void setAdults(int adults) {
        this.adults = adults;
    }

    public int getChildren() {
        return children;
    }

    public void setChildren(int children) {
        this.children = children;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
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

}
