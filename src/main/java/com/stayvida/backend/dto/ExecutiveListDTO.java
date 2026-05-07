package com.stayvida.backend.dto;

public class ExecutiveListDTO {

    private int userId;
    private String email;
    private String role;
    private String phoneNumber;
    private String name;
    private String referralCode;
    private boolean isEnable;

    // Getters
    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public boolean getIsEnable() {
        return isEnable;
    }

    // setter
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public void setIsEnable(boolean isEnable) {
        this.isEnable = isEnable;
    }
}
