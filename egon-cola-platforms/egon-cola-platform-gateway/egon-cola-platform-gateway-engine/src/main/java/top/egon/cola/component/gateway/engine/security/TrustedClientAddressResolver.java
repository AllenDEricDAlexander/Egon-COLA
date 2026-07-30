package top.egon.cola.component.gateway.engine.security;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class TrustedClientAddressResolver {

    private final List<Cidr> trustedProxies;

    public TrustedClientAddressResolver(List<String> trustedProxyCidrs) {
        if (trustedProxyCidrs == null) {
            trustedProxies = List.of();
            return;
        }
        trustedProxies = trustedProxyCidrs.stream()
                .map(Cidr::parse)
                .toList();
    }

    public InetAddress resolve(
            InetSocketAddress peer,
            Map<String, List<String>> headers) {
        if (peer == null || peer.getAddress() == null) {
            throw new IllegalArgumentException(
                    "resolved peer address is required"
            );
        }
        InetAddress peerAddress = peer.getAddress();
        if (!trusted(peerAddress)) {
            return peerAddress;
        }
        Optional<InetAddress> forwarded = firstHeader(
                headers,
                "forwarded"
        ).flatMap(this::forwardedAddress);
        if (forwarded.isPresent()) {
            return forwarded.get();
        }
        return firstHeader(headers, "x-forwarded-for")
                .flatMap(this::forwardedForAddress)
                .orElse(peerAddress);
    }

    public boolean trusted(InetAddress address) {
        return trustedProxies.stream().anyMatch(cidr -> cidr.contains(address));
    }

    private Optional<String> firstHeader(
            Map<String, List<String>> headers,
            String expected) {
        if (headers == null) {
            return Optional.empty();
        }
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(expected))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst();
    }

    private Optional<InetAddress> forwardedAddress(String value) {
        if (value == null || value.length() > 2048) {
            return Optional.empty();
        }
        String first = value.split(",", 2)[0];
        for (String parameter : first.split(";")) {
            String[] pair = parameter.trim().split("=", 2);
            if (pair.length == 2
                    && "for".equals(pair[0].toLowerCase(Locale.ROOT))) {
                return literal(addressPart(unquote(pair[1].trim())));
            }
        }
        return Optional.empty();
    }

    private Optional<InetAddress> forwardedForAddress(String value) {
        if (value == null || value.length() > 2048) {
            return Optional.empty();
        }
        return literal(addressPart(value.split(",", 2)[0].trim()));
    }

    private String unquote(String value) {
        return value.length() >= 2
                && value.startsWith("\"")
                && value.endsWith("\"")
                ? value.substring(1, value.length() - 1)
                : value;
    }

    private String addressPart(String value) {
        if (value.startsWith("[")) {
            int close = value.indexOf(']');
            return close > 1 ? value.substring(1, close) : "";
        }
        int colon = value.indexOf(':');
        return colon > 0 && value.indexOf(':', colon + 1) < 0
                ? value.substring(0, colon)
                : value;
    }

    private Optional<InetAddress> literal(String value) {
        if (value == null
                || value.isBlank()
                || value.contains("%")
                || (!ipv4(value) && !value.contains(":"))) {
            return Optional.empty();
        }
        try {
            return Optional.of(InetAddress.getByName(value));
        } catch (UnknownHostException invalid) {
            return Optional.empty();
        }
    }

    private boolean ipv4(String value) {
        String[] segments = value.split("\\.", -1);
        if (segments.length != 4) {
            return false;
        }
        for (String segment : segments) {
            if (!segment.matches("[0-9]{1,3}")
                    || Integer.parseInt(segment) > 255) {
                return false;
            }
        }
        return true;
    }

    private record Cidr(byte[] network, int prefixLength) {

        private static Cidr parse(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "trusted proxy CIDR is required"
                );
            }
            String[] parts = value.trim().split("/", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "trusted proxy must be CIDR"
                );
            }
            try {
                InetAddress address = InetAddress.getByName(parts[0]);
                int prefix = Integer.parseInt(parts[1]);
                int maximum = address.getAddress().length * 8;
                if (prefix < 0 || prefix > maximum) {
                    throw new IllegalArgumentException(
                            "invalid trusted proxy prefix"
                    );
                }
                return new Cidr(address.getAddress(), prefix);
            } catch (UnknownHostException | NumberFormatException invalid) {
                throw new IllegalArgumentException(
                        "invalid trusted proxy CIDR",
                        invalid
                );
            }
        }

        private boolean contains(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int index = 0; index < fullBytes; index++) {
                if (candidate[index] != network[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xff << (8 - remainingBits);
            return (candidate[fullBytes] & mask)
                    == (network[fullBytes] & mask);
        }
    }
}
