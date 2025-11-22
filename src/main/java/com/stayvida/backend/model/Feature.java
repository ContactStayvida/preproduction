package com.stayvida.backend.model;

public class Feature {
    private int feature_id;
    private String name;
    private String status; // enable / disable

    public int getFeature_id() { return feature_id; }
    public void setFeature_id(int feature_id) { this.feature_id = feature_id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
