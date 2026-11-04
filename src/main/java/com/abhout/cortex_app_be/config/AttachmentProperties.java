package com.abhout.cortex_app_be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.attachments")
@Setter
@Getter
public class AttachmentProperties {
    private String dir = "./data/attachments";
    private DataSize maxSize = DataSize.ofMegabytes(10);
}
