package com.abhout.cortex_app_be.common;

import com.abhout.cortex_app_be.common.filters.RequestLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class ObservabilityConfig {
    @Bean
    FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter(){
        FilterRegistrationBean<RequestLoggingFilter> bean = new FilterRegistrationBean<>(new RequestLoggingFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
