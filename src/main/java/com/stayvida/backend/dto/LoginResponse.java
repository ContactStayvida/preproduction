package com.stayvida.backend.dto;

public class LoginResponse {
    private boolean success;
    private String token;
    private Long id;
    private String email;
    private String role;
    private String message;
    private boolean profileExists;


    public LoginResponse() {
    }

    public LoginResponse(boolean success, String token, Long id , String email,  String role, String message) {
        this.success = success;
        this.token = token;
        this.id = id;
        this.email = email;
        this.role = role;
        this.message = message;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getToken() {
        return token;
    }
    public Long getuserID() {
        return id;
    }


    public String getEmail() {
        return email;
    }

       public String getRole() {
        return role;
    }


    public String getMessage() {
        return message;
    }
    public boolean isProfileExists() {
        return profileExists;
    }   
    public void setProfileExists(boolean profileExists) {
        this.profileExists = profileExists;
    }    
 
}
