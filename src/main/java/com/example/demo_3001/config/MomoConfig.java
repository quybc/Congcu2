package com.example.demo_3001.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "momo")
@Getter
@Setter
public class MomoConfig {
    private String momoApiUrl;
    private String secretKey;
    private String accessKey;
    private String returnUrl;
    private String notifyUrl;
    private String partnerCode;
    private String partnerName;
    private String storeId;
    private String requestType;
}
