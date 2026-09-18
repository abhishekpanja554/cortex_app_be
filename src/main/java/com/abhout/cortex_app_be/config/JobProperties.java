package com.abhout.cortex_app_be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jobs")
@Setter
@Getter
public class JobProperties {
    private String internalApiKey;
    private String queueKey = "jobs:embedding";
    private Duration timeoutThreshold = Duration.ofMinutes(5);
}
