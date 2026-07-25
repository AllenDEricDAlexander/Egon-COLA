package top.egon.cola.component.gateway.core.http;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HttpRequestNormalizer {

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade"
    );

    private final int maxHeaderCount;

    private final int maxHeaderBytes;

    public HttpRequestNormalizer(int maxHeaderCount, int maxHeaderBytes) {
        if (maxHeaderCount < 1 || maxHeaderBytes < 256) {
            throw new IllegalArgumentException("invalid HTTP header limits");
        }
        this.maxHeaderCount = maxHeaderCount;
        this.maxHeaderBytes = maxHeaderBytes;
    }

    public NormalizedHttpRequest normalize(
            String method,
            String host,
            String uri,
            Map<String, List<String>> headers) {
        String normalizedMethod = required(method, "method")
                .toUpperCase(Locale.ROOT);
        String normalizedHost = normalizeHost(host);
        String requestUri = required(uri, "uri");
        int queryOffset = requestUri.indexOf('?');
        String rawPath = queryOffset < 0
                ? requestUri
                : requestUri.substring(0, queryOffset);
        String query = queryOffset < 0
                ? ""
                : requestUri.substring(queryOffset + 1);
        return new NormalizedHttpRequest(
                normalizedMethod,
                normalizedHost,
                rawPath,
                normalizePath(rawPath),
                query,
                normalizeHeaders(headers)
        );
    }

    public String normalizePath(String rawPath) {
        String path = required(rawPath, "path");
        if (!path.startsWith("/")) {
            reject("path must start with '/'");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(path.length());
        for (int index = 0; index < path.length(); index++) {
            char current = path.charAt(index);
            if (current == '%') {
                if (index + 2 >= path.length()) {
                    reject("incomplete percent encoding");
                }
                int high = Character.digit(path.charAt(index + 1), 16);
                int low = Character.digit(path.charAt(index + 2), 16);
                if (high < 0 || low < 0) {
                    reject("invalid percent encoding");
                }
                int decoded = high * 16 + low;
                if (decoded == '/' || decoded == '\\' || decoded == '%'
                        || decoded == 0) {
                    reject("encoded separator, percent or NUL is not allowed");
                }
                bytes.write(decoded);
                index += 2;
            } else {
                byte[] encoded = String.valueOf(current)
                        .getBytes(StandardCharsets.UTF_8);
                bytes.writeBytes(encoded);
            }
        }
        String decoded;
        try {
            decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray()))
                    .toString();
        } catch (CharacterCodingException exception) {
            reject("path is not valid UTF-8");
            throw new IllegalStateException(exception);
        }
        if (decoded.indexOf('\0') >= 0 || decoded.indexOf('\\') >= 0) {
            reject("path contains an unsafe character");
        }
        List<String> segments = new ArrayList<>();
        for (String segment : decoded.split("/", -1)) {
            if (".".equals(segment) || "..".equals(segment)) {
                reject("path traversal is not allowed");
            }
            segments.add(segment);
        }
        return String.join("/", segments);
    }

    private Map<String, List<String>> normalizeHeaders(
            Map<String, List<String>> source) {
        if (source == null || source.size() > maxHeaderCount) {
            reject("too many headers");
        }
        int totalBytes = 0;
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            String name = required(entry.getKey(), "header name")
                    .toLowerCase(Locale.ROOT);
            if (HOP_BY_HOP.contains(name)) {
                continue;
            }
            List<String> values = List.copyOf(entry.getValue());
            totalBytes += name.getBytes(StandardCharsets.UTF_8).length;
            for (String value : values) {
                if (value == null || value.indexOf('\r') >= 0
                        || value.indexOf('\n') >= 0) {
                    reject("invalid header value");
                }
                totalBytes += value.getBytes(StandardCharsets.UTF_8).length;
            }
            normalized.put(name, values);
        }
        if (totalBytes > maxHeaderBytes) {
            reject("header bytes exceed configured limit");
        }
        return Map.copyOf(normalized);
    }

    private String normalizeHost(String host) {
        String normalized = required(host, "host")
                .toLowerCase(Locale.ROOT);
        if (normalized.endsWith(":80")) {
            return normalized.substring(0, normalized.length() - 3);
        }
        if (normalized.endsWith(":443")) {
            return normalized.substring(0, normalized.length() - 4);
        }
        if (normalized.contains("/") || normalized.contains("\\")
                || normalized.contains("@") || normalized.contains(" ")) {
            reject("invalid host");
        }
        return normalized;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            reject(field + " is required");
        }
        return value.trim();
    }

    private static void reject(String message) {
        throw new GatewayRequestRejectedException(
                "GATEWAY_REQUEST_INVALID",
                400,
                message
        );
    }
}
