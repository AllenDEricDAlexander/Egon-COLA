package top.egon.cola.component.ddc.management.client;

import java.net.URI;
import java.time.Duration;

public record DdcManagementClientProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        Duration connectTimeout,
        Duration readTimeout
) {

    public DdcManagementClientProperties {
        endpoint = normalizeEndpoint(endpoint);
        accessKey = requireText(accessKey, "accessKey");
        requireText(secretKey, "secretKey");
        connectTimeout = requirePositive(connectTimeout, "connectTimeout");
        readTimeout = requirePositive(readTimeout, "readTimeout");
    }

    @Override
    public String toString() {
        return "DdcManagementClientProperties[endpoint="
                + endpoint
                + ", accessKey="
                + accessKey
                + ", secretKey=******, connectTimeout="
                + connectTimeout
                + ", readTimeout="
                + readTimeout
                + "]";
    }

    private static String normalizeEndpoint(String value) {
        String endpoint = requireText(value, "endpoint");
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("endpoint must be a valid HTTP URI", exception);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("endpoint must use HTTP or HTTPS");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("endpoint host is required");
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "endpoint must not contain user info, query, or fragment"
            );
        }
        if (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath())) {
            throw new IllegalArgumentException("endpoint must not contain a context path");
        }
        return endpoint.endsWith("/")
                ? endpoint.substring(0, endpoint.length() - 1)
                : endpoint;
    }

    private static Duration requirePositive(Duration value, String fieldName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
