package com.abhout.cortex_app_be.common.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RequestLoggingFilterTest {
    private final RequestLoggingFilter filter =
            new RequestLoggingFilter(new LogSanitizer(JsonMapper.builder().build(), 10_000));
    private final Logger filterLogger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        filterLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        filterLogger.detachAppender(appender);
    }

    private static MockHttpServletRequest jsonPost(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Bearer secret-token-value");
        return request;
    }

    private String allLogOutput() {
        return appender.list.stream()
                .map(e -> e.getFormattedMessage() + " " + e.getMarkerList())
                .reduce("", String::concat);
    }

    @Test
    void bodyIsStillReadableDownstreamAndResponseIsCopiedThrough() throws Exception {
        MockHttpServletRequest request = jsonPost("{\"email\":\"a@b.c\",\"password\":\"hunter2\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenByController = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            seenByController.set(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            res.setContentType("application/json");
            res.getWriter().write("{\"data\":{\"accessToken\":\"abc.def.ghi\"}}");
        };
        filter.doFilter(request, response, chain);

        assertEquals("{\"email\":\"a@b.c\",\"password\":\"hunter2\"}", seenByController.get());
        assertEquals("{\"data\":{\"accessToken\":\"abc.def.ghi\"}}", response.getContentAsString());
        assertNotNull(response.getHeader("X-Request-Id"));

        String logs = allLogOutput();
        assertFalse(logs.contains("hunter2"), logs);
        assertFalse(logs.contains("abc.def.ghi"), logs);
        assertFalse(logs.contains("secret-token-value"), logs);
        assertTrue(logs.contains("Bearer ***"), logs);
        assertNull(MDC.get("requestId"), "MDC must be cleared after the request");
    }

    @Test
    void exceptionEscapingTheChainIsLoggedWithStackTraceAndRethrown() {
        MockHttpServletRequest request = jsonPost("{}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        IllegalStateException boom = new IllegalStateException("boom");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> filter.doFilter(request, response, (req, res) -> { throw boom; }));
        assertSame(boom, thrown);

        List<ILoggingEvent> errors = appender.list.stream().filter(e -> e.getLevel() == Level.ERROR).toList();
        assertEquals(2, errors.size(), allLogOutput());
        assertEquals("boom", errors.get(0).getThrowableProxy().getMessage());
        assertTrue(errors.get(1).getFormattedMessage().startsWith("HTTP response 500"), errors.get(1).getFormattedMessage());
        assertNull(MDC.get("requestId"), "MDC must be cleared even when the chain throws");
    }

    @Test
    void unknownCharsetDoesNotBreakTheRequest() throws Exception {
        MockHttpServletRequest request = jsonPost("{\"a\":1}");
        request.setContentType("application/json; charset=bogus");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> reachedController = new AtomicReference<>(false);

        filter.doFilter(request, response, (req, res) -> reachedController.set(true));

        assertTrue(reachedController.get());
    }
}
