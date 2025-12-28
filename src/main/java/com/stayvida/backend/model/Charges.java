package com.stayvida.backend.model;

public class Charges {
    private Integer charge_ID;
    private String type;
    private double Value;

    public Integer getCharge_ID() {
        return charge_ID;
    }

    public void setCharge_ID(Integer charge_ID) {
        this.charge_ID = charge_ID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getValue() {
        return Value;
    }

    public void setValue(double value) {
        Value = value;
    }
}
