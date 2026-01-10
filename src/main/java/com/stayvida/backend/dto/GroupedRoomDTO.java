package com.stayvida.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class GroupedRoomDTO {

    private String type;
    private List<RoomBasicDTO> rooms;

    private BigDecimal price;
    private BigDecimal platformCharges;
    private BigDecimal taxRate;
    private BigDecimal advanceRate;
    private BigDecimal totalAmount;
    private BigDecimal advanceAmount;

    private int adultsMax;
    private int childrenMax;
    private int bedCount;

    private List<String> features;
    private List<String> roomImages;

    // getters & setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<RoomBasicDTO> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomBasicDTO> rooms) {
        this.rooms = rooms;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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
