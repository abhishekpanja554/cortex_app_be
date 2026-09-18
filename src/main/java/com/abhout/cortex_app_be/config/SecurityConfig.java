package com.abhout.cortex_app_be.config;

import com.abhout.cortex_app_be.auth.security.JWTAuthSecurityFilter;
import com.abhout.cortex_app_be.jobs.security.InternalApiKeyFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JWTAuthSecurityFilter jwtFilter){
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(
                                        "/auth/**",
                                        "/internal/**",
                                        "/error",
                                        "/actuator/health",
                                        "/actuator/metrics")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        e -> e.authenticationEntryPoint(
                                (
                                        req,
                                        res,
                                        ex
                                ) -> {
                                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                                }
                ));
        return http.build();
    }

    @Bean
    FilterRegistrationBean<InternalApiKeyFilter> internalApiKeyFilter(JobProperties jobProperties){
        FilterRegistrationBean<InternalApiKeyFilter> bean = new FilterRegistrationBean<>(
                new InternalApiKeyFilter(jobProperties.getInternalApiKey())
        );
        bean.addUrlPatterns("/internal/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return bean;
    }
}
