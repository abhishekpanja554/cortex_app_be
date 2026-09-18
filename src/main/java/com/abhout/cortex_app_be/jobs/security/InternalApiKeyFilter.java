package com.abhout.cortex_app_be.jobs.security;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

import java.io.IOException;
import java.security.MessageDigest;

public class InternalApiKeyFilter implements Filter {
    private final String internalApiKey;
    private static final String HEADER = "X-Internal-Api-Key";

    public InternalApiKeyFilter(String internalApiKey){
        this.internalApiKey = internalApiKey;
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String apiKey = httpRequest.getHeader(HEADER);
        if( apiKey == null || apiKey.isBlank() || internalApiKey == null || internalApiKey.isBlank() ||
            !MessageDigest.isEqual(apiKey.getBytes(), internalApiKey.getBytes())){
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }
}
