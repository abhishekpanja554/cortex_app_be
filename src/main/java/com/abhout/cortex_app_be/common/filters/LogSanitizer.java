package com.abhout.cortex_app_be.common.filters;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// Masks credentials and caps size; masking lists are hardcoded on purpose.
public class LogSanitizer {
    private static final String MASK = "***";
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "proxy-authorization", "cookie", "set-cookie", "x-internal-api-key"
    );
    // Matched case-insensitively.
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "accesstoken", "refreshtoken", "token", "secret", "apikey", "internalapikey"
    );

    public record LoggedBody(String content, boolean json) {}

    private final JsonMapper jsonMapper;
    private final int maxBodyLength;

    public LogSanitizer(JsonMapper jsonMapper, int maxBodyLength) {
        this.jsonMapper = jsonMapper;
        this.maxBodyLength = maxBodyLength;
    }

    public Map<String, String> headers(Collection<String> names, Function<String, Collection<String>> values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : names) {
            String value = String.join(", ", values.apply(name));
            result.put(name, SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT)) ? maskHeader(value) : value);
        }
        return result;
    }

    // Keeps the scheme visible, e.g. "Bearer ***".
    private String maskHeader(String value) {
        int space = value.indexOf(' ');
        return space > 0 ? value.substring(0, space) + " " + MASK : MASK;
    }

    // JSON, XML, text/*; forms excluded since buffering them breaks getParameter().
    public boolean isTextual(String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            String subtype = mediaType.getSubtype();
            return "text".equals(mediaType.getType())
                    || "json".equals(subtype) || subtype.endsWith("+json")
                    || "xml".equals(subtype) || subtype.endsWith("+xml");
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }

    // Null when there's no body.
    public LoggedBody body(byte[] bytes, String contentType, String characterEncoding) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        if (!isTextual(contentType)) {
            return new LoggedBody("[not logged: " + contentType + ", " + bytes.length + " bytes]", false);
        }
        String text = new String(bytes, charsetOrUtf8(characterEncoding));

        boolean json = false;
        try {
            JsonNode root = jsonMapper.readTree(text);
            mask(root);
            text = jsonMapper.writeValueAsString(root);
            json = true;
        } catch (JacksonException ex) {
            // Not JSON; log as plain text.
        }

        if (text.length() > maxBodyLength) {
            // Truncated JSON is invalid, so log as a string.
            return new LoggedBody(text.substring(0, maxBodyLength)
                    + "...[truncated, " + text.length() + " chars total]", false);
        }
        return new LoggedBody(text, json);
    }

    // Unknown client charsets fall back to UTF-8.
    private static Charset charsetOrUtf8(String name) {
        if (name == null) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(name);
        } catch (IllegalArgumentException ex) {
            return StandardCharsets.UTF_8;
        }
    }

    private void mask(JsonNode node) {
        if (node instanceof ObjectNode object) {
            for (String name : List.copyOf(object.propertyNames())) {
                if (SENSITIVE_FIELDS.contains(name.toLowerCase(Locale.ROOT))) {
                    object.put(name, MASK);
                } else {
                    mask(object.get(name));
                }
            }
        } else if (node instanceof ArrayNode array) {
            for (JsonNode child : array) {
                mask(child);
            }
        }
    }
}
