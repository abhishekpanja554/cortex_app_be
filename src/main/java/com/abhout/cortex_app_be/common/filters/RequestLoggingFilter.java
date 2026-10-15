package com.abhout.cortex_app_be.common.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

// Logs each request and its response, with headers and bodies.
public class RequestLoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";
    // Set by JWTAuthSecurityFilter, SecurityContextHolder is already cleared by the time we log.
    public static final String MDC_USER_ID_KEY = "userId";

    private final LogSanitizer sanitizer;

    public RequestLoggingFilter(LogSanitizer sanitizer) {
        this.sanitizer = sanitizer;
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if(requestId == null || requestId.isBlank()){
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

        long startTime = System.currentTimeMillis();

        try {
            CachedBodyHttpServletRequest cachedRequest = sanitizer.isTextual(httpRequest.getContentType())
                    ? new CachedBodyHttpServletRequest(httpRequest)
                    : null;
            HttpServletRequest requestToUse = cachedRequest != null ? cachedRequest : httpRequest;
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

            safely(() -> logRequest(requestToUse, cachedRequest));

            Throwable failure = null;
            try {
                chain.doFilter(requestToUse, responseWrapper);
            } catch (IOException | ServletException | RuntimeException | Error ex) {
                failure = ex;
                logger.error("Unhandled exception escaped the filter chain on {} {}",
                        requestToUse.getMethod(), requestToUse.getRequestURI(), ex);
                throw ex;
            } finally {
                Throwable finalFailure = failure;
                safely(() -> logResponse(requestToUse, responseWrapper, startTime, finalFailure));
                if (failure == null) {
                    responseWrapper.copyBodyToResponse();
                }
            }
        } finally {
            MDC.clear();
        }
    }

    private void logRequest(HttpServletRequest request, CachedBodyHttpServletRequest cachedRequest) {
        LogSanitizer.LoggedBody body;
        if (cachedRequest != null) {
            body = sanitizer.body(cachedRequest.getBody(), request.getContentType(), request.getCharacterEncoding());
        } else if (request.getContentLengthLong() > 0) {
            body = new LogSanitizer.LoggedBody(
                    "[not logged: " + request.getContentType() + ", " + request.getContentLengthLong() + " bytes]",
                    false);
        } else {
            body = null;
        }

        LogstashMarker fields = Markers.append("method", request.getMethod())
                .and(Markers.append("path", request.getRequestURI()));
        fields = withField(fields, "query", request.getQueryString());
        fields = fields
                .and(Markers.append("clientIp", request.getRemoteAddr()))
                .and(Markers.append("requestHeaders", sanitizer.headers(
                        Collections.list(request.getHeaderNames()),
                        name -> Collections.list(request.getHeaders(name)))));
        fields = withBody(fields, "requestBody", body);

        logger.info(fields, "HTTP request  {} {}", request.getMethod(), pathWithQuery(request));
    }

    private void logResponse(
            HttpServletRequest request,
            ContentCachingResponseWrapper response,
            long startTime,
            Throwable failure
    ) {
        long durationMs = System.currentTimeMillis() - startTime;
        int status = failure != null ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus();

        String route = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE) instanceof HandlerMethod method
                ? method.getBeanType().getSimpleName() + "#" + method.getMethod().getName()
                : null;

        LogstashMarker fields = Markers.append("method", request.getMethod())
                .and(Markers.append("path", request.getRequestURI()));
        fields = withField(fields, "route", route);
        fields = withField(fields, "handler", handler);
        fields = fields
                .and(Markers.append("status", status))
                .and(Markers.append("durationMs", durationMs));
        fields = withField(fields, "userId", MDC.get(MDC_USER_ID_KEY));
        fields = fields.and(Markers.append("responseHeaders", sanitizer.headers(
                response.getHeaderNames(),
                response::getHeaders)));
        fields = withBody(fields, "responseBody",
                sanitizer.body(response.getContentAsByteArray(), response.getContentType(), response.getCharacterEncoding()));

        Level level = status >= 500 ? Level.ERROR : status >= 400 ? Level.WARN : Level.INFO;
        logger.atLevel(level)
                .addMarker(fields)
                .log("HTTP response {} {} {} ({} ms)", status, request.getMethod(), pathWithQuery(request), durationMs);
    }

    private static void safely(Runnable logStatement) {
        try {
            logStatement.run();
        } catch (RuntimeException ex) {
            logger.error("Failed to write the request/response log line", ex);
        }
    }

    private static LogstashMarker withField(LogstashMarker fields, String name, Object value) {
        return value == null ? fields : fields.and(Markers.append(name, value));
    }

    private static LogstashMarker withBody(LogstashMarker fields, String name, LogSanitizer.LoggedBody body) {
        if (body == null) {
            return fields;
        }
        return fields.and(body.json()
                ? Markers.appendRaw(name, body.content())
                : Markers.append(name, body.content()));
    }

    private static String pathWithQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }
}
