package top.egon.cola.component.gateway.mcp.remote;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

/**
 * Shared control-plane and data-plane policy for remote MCP endpoints.
 */
public final class McpRemoteEndpointValidator {

    private McpRemoteEndpointValidator() {
    }

    public static URI requireSafe(String endpointReference) {
        URI endpoint;
        try {
            endpoint = URI.create(endpointReference);
        } catch (RuntimeException failure) {
            throw invalidEndpoint(failure);
        }
        if (!endpoint.isAbsolute()
                || endpoint.getUserInfo() != null
                || endpoint.getHost() == null
                || endpoint.getQuery() != null
                || endpoint.getFragment() != null
                || !("http".equalsIgnoreCase(endpoint.getScheme())
                || "https".equalsIgnoreCase(endpoint.getScheme()))) {
            throw invalidEndpoint(null);
        }
        validatePath(endpoint.getRawPath());
        validateHost(endpoint.getHost());
        return endpoint;
    }

    private static void validatePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return;
        }
        String normalized = rawPath.toLowerCase(Locale.ROOT);
        if (normalized.contains("..")
                || normalized.contains("\\")
                || normalized.contains("%2e")
                || normalized.contains("%2f")
                || normalized.contains("%5c")) {
            throw invalidEndpoint(null);
        }
    }

    private static void validateHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.equals("metadata")
                || normalized.startsWith("metadata.")) {
            throw invalidEndpoint(null);
        }
        int[] ipv4 = parseIpv4(normalized);
        if (ipv4 != null) {
            if (ipv4[0] == 0
                    || ipv4[0] >= 224
                    || (ipv4[0] == 169 && ipv4[1] == 254)
                    || (ipv4[0] == 255 && ipv4[1] == 255
                    && ipv4[2] == 255 && ipv4[3] == 255)) {
                throw invalidEndpoint(null);
            }
            return;
        }
        if (normalized.indexOf(':') >= 0) {
            validateIpv6(normalized);
            return;
        }
        if (normalized.chars().allMatch(Character::isDigit)) {
            throw invalidEndpoint(null);
        }
    }

    private static int[] parseIpv4(String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        int[] result = new int[4];
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isEmpty()
                    || part.length() > 3
                    || (part.length() > 1 && part.charAt(0) == '0')
                    || !part.chars().allMatch(Character::isDigit)) {
                throw invalidEndpoint(null);
            }
            int value = Integer.parseInt(part);
            if (value > 255) {
                throw invalidEndpoint(null);
            }
            result[index] = value;
        }
        return result;
    }

    private static void validateIpv6(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            if (!(address instanceof Inet6Address)
                    || address.isAnyLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isMulticastAddress()) {
                throw invalidEndpoint(null);
            }
        } catch (IllegalArgumentException failure) {
            throw failure;
        } catch (Exception failure) {
            throw invalidEndpoint(failure);
        }
    }

    private static IllegalArgumentException invalidEndpoint(
            Throwable cause) {
        return new IllegalArgumentException(
                "remote MCP endpoint must be a path-safe HTTP(S) URI "
                        + "without embedded credentials, query, fragment, "
                        + "or link-local/metadata address",
                cause
        );
    }
}
