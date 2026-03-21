package com.stayvida.backend.dto;

import java.math.BigDecimal;

public class UpdateAmountRequest {

    private String type;
    private BigDecimal value;

    // getters setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}