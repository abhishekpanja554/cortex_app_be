package com.abhout.cortex_app_be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.logging")
@Setter
@Getter
public class LoggingProperties {
    // "json" or "pretty"; consumed by logback-spring.xml.
    private String format = "json";
    // Logged bodies are truncated beyond this many chars.
    private int maxBodyLength = 10_000;
}
