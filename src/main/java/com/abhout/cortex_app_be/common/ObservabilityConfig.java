package com.abhout.cortex_app_be.common;

import com.abhout.cortex_app_be.common.filters.LogSanitizer;
import com.abhout.cortex_app_be.common.filters.RequestLoggingFilter;
import com.abhout.cortex_app_be.config.LoggingProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ObservabilityConfig {
    @Bean
    FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter(JsonMapper jsonMapper, LoggingProperties loggingProperties){
        LogSanitizer sanitizer = new LogSanitizer(jsonMapper, loggingProperties.getMaxBodyLength());
        FilterRegistrationBean<RequestLoggingFilter> bean = new FilterRegistrationBean<>(new RequestLoggingFilter(sanitizer));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
