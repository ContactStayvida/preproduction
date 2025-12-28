package com.stayvida.backend.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String key;

    @Value("${razorpay.key.secret}")
    private String secret;

    public String getKey() {
        return key;
    }

    public String getSecret() {
        return secret;
    }
}
