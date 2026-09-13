package com.abhout.cortex_app_be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JWTProperties {
    private String secret;
    private Duration accessTokenTtl;
    private Duration refreshTokenTtl;
}
