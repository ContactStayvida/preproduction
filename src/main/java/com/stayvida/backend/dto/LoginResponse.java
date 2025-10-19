package com.stayvida.backend.dto;

public class LoginResponse {
    private boolean success;
    private String token;
    private String username;
    private String email;
    private String role;
    private String message;


    public LoginResponse() {
    }

    public LoginResponse(boolean success, String token, String username, String email,  String role, String message) {
        this.success = success;
        this.token = token;
        this.username = username;
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

    public String getUsername() {
        return username;
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
 
}
