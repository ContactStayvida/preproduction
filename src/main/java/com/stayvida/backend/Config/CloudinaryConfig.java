package com.stayvida.backend.Config;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "damm4pumh",
                "api_key", "513981373163998",
                "api_secret", "HO_Wak6vYSJVd7fN-_t_t3nhsPg"));
    }
}
