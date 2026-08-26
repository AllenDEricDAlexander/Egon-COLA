package top.egon.cola.component.gateway.admin.openapi.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Resolves a Provider host and fails closed unless every answer is allowlisted.
 *
 * <p>The caller invokes this policy before token acquisition and immediately
 * before connecting. The second resolution is intentionally not cached so a
 * DNS rebinding answer cannot silently bypass the CIDR boundary.</p>
 */
public class GatewayOpenApiDnsPolicy {

    private final List<String> allowedCidrs;

    private final Function<String, List<InetAddress>> resolver;

    /**
     * Creates a production policy using JDK DNS resolution.
     *
     * @param allowedCidrs non-empty CIDR allowlist
     */
    public GatewayOpenApiDnsPolicy(List<String> allowedCidrs) {
        this(
                allowedCidrs,
                host -> {
                    try {
                        return Arrays.asList(InetAddress.getAllByName(host));
                    } catch (UnknownHostException failure) {
                        throw new GatewayOpenApiFetchException(
                                "GATEWAY_OPENAPI_DNS_FAILED",
                                true,
                                "provider DNS resolution failed"
                        );
                    }
                }
        );
    }

    /**
     * Creates a policy with an injectable resolver for deterministic tests.
     *
     * @param allowedCidrs non-empty CIDR allowlist
     * @param resolver host resolver
     */
    public GatewayOpenApiDnsPolicy(
            List<String> allowedCidrs,
            Function<String, List<InetAddress>> resolver) {
        if (allowedCidrs == null || allowedCidrs.isEmpty()) {
            throw new IllegalArgumentException(
                    "OpenAPI DNS allowlist must not be empty"
            );
        }
        this.allowedCidrs = allowedCidrs.stream()
                .map(value -> required(value, "allowed CIDR"))
                .toList();
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.allowedCidrs.forEach(GatewayOpenApiDnsPolicy::validateCidr);
    }

    /**
     * Resolves and validates every current address for a host.
     *
     * @param host bare Provider host
     * @return all validated addresses
     */
    public List<InetAddress> resolveAndValidate(String host) {
        String normalizedHost = required(host, "host");
        List<InetAddress> addresses = resolver.apply(normalizedHost);
        if (addresses == null || addresses.isEmpty()) {
            throw new GatewayOpenApiFetchException(
                    "GATEWAY_OPENAPI_DNS_EMPTY",
                    true,
                    "provider DNS returned no address"
            );
        }
        List<InetAddress> normalized = addresses.stream()
                .map(address -> Objects.requireNonNull(address, "address"))
                .toList();
        if (normalized.stream().anyMatch(
                address -> !allowed(address)
        )) {
            throw new GatewayOpenApiFetchException(
                    "GATEWAY_OPENAPI_TARGET_FORBIDDEN",
                    false,
                    "provider target is not allowlisted"
            );
        }
        return normalized;
    }

    private boolean allowed(InetAddress address) {
        return allowedCidrs.stream().anyMatch(cidr -> matches(address, cidr));
    }

    private static boolean matches(InetAddress address, String cidr) {
        int slash = cidr.indexOf('/');
        String networkText = cidr.substring(0, slash);
        int prefix = Integer.parseInt(cidr.substring(slash + 1));
        try {
            byte[] addressBytes = address.getAddress();
            byte[] networkBytes = InetAddress.getByName(networkText)
                    .getAddress();
            if (addressBytes.length != networkBytes.length) {
                return false;
            }
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int index = 0; index < fullBytes; index++) {
                if (addressBytes[index] != networkBytes[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (addressBytes[fullBytes] & mask)
                    == (networkBytes[fullBytes] & mask);
        } catch (UnknownHostException failure) {
            throw new IllegalArgumentException(
                    "invalid CIDR network",
                    failure
            );
        }
    }

    private static void validateCidr(String value) {
        int slash = value.indexOf('/');
        if (slash <= 0 || slash == value.length() - 1
                || value.indexOf('/', slash + 1) >= 0) {
            throw new IllegalArgumentException("invalid CIDR: " + value);
        }
        try {
            InetAddress network = InetAddress.getByName(
                    value.substring(0, slash)
            );
            int prefix = Integer.parseInt(value.substring(slash + 1));
            int max = network.getAddress().length * 8;
            if (prefix < 0 || prefix > max) {
                throw new IllegalArgumentException("invalid CIDR: " + value);
            }
        } catch (UnknownHostException | NumberFormatException failure) {
            throw new IllegalArgumentException("invalid CIDR: " + value, failure);
        }
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > 256) {
            throw new IllegalArgumentException(
                    field + " must contain 1..256 characters"
            );
        }
        return normalized;
    }
}
