package com.stayvida.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

public class RoomDTO {
    // private String roomId;
    // private Integer room_NO;
    // private String hotelId;
    private String type;
    private BigDecimal price;
    private BigDecimal platformCharges;
    private BigDecimal taxRate;
    private BigDecimal advanceRate;
    private long stayDuration;
    private int adultsMax;
    private int childrenMax;
    private int bedCount;
    private List<String> features;
    private List<String> roomImages;
    private BigDecimal totalAmount;
    private BigDecimal advanceAmount;

    @JsonPropertyOrder({ "roomId", "room_NO", "hotelId", "type", "price", "platformCharges",
            "taxRate", "advanceRate", "totalAmount", "advanceAmount", "stayDuration", "adultsMax", "childrenMax",
            "bedCount",
            "features",
            "roomImages" })
    public RoomDTO(String type, BigDecimal price,
            BigDecimal platformCharges, BigDecimal taxRate, BigDecimal advanceRate, BigDecimal totalAmount,
            BigDecimal advanceAmount,
            long stayDuration, int adultsMax,
            int childrenMax,
            int bedCount)// String roomId, Integer room_NO, String hotelId,
    {
        // this.roomId = roomId;
        // this.room_NO = room_NO;
        // this.hotelId = hotelId;
        this.type = type;
        this.price = price;
        this.platformCharges = platformCharges;
        this.taxRate = taxRate;
        this.advanceRate = advanceRate;
        this.totalAmount = totalAmount;
        this.advanceAmount = advanceAmount;
        this.adultsMax = adultsMax;
        this.childrenMax = childrenMax;
        this.bedCount = bedCount;
        this.stayDuration = stayDuration;

    }

    // // Getters & Setters
    // public String getRoomId() {
    // return roomId;
    // }

    // public void setRoomId(String roomId) {
    // this.roomId = roomId;
    // }

    // public void setRoom_NO(Integer room_NO) {
    // this.room_NO = room_NO;
    // }

    // public Integer getRoom_NO() {
    // return room_NO;
    // }

    // public String getHotelId() {
    // return hotelId;
    // }

    // public void setHotelId(String hotelId) {
    // this.hotelId = hotelId;
    // }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getPlatformCharges() {
        return platformCharges;
    }

    public void setPlatformCharges(BigDecimal platformCharges) {
        this.platformCharges = platformCharges;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getAdvanceRate() {
        return advanceRate;
    }

    public void setAdvanceRate(BigDecimal advanceRate) {
        this.advanceRate = advanceRate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getAdvanceAmount() {
        return advanceAmount;
    }

    public void setAdvanceAmount(BigDecimal advanceAmount) {
        this.advanceAmount = advanceAmount;
    }

    public long getStayDuration() {
        return stayDuration;
    }

    public void setStayDuration(long stayDuration) {
        this.stayDuration = stayDuration;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getAdultsMax() {
        return adultsMax;
    }

    public void setAdultsMax(int adultsMax) {
        this.adultsMax = adultsMax;
    }

    public int getChildrenMax() {
        return childrenMax;
    }

    public void setChildrenMax(int childrenMax) {
        this.childrenMax = childrenMax;
    }

    public int getBedCount() {
        return bedCount;
    }

    public void setBedCount(int bedCount) {
        this.bedCount = bedCount;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public List<String> getRoomImages() {
        return roomImages;
    }

    public void setRoomImages(List<String> roomImages) {
        this.roomImages = roomImages;
    }

}
