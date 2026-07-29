package top.egon.cola.component.accessguard.key;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public final class TrustedProxyMatcher {

    private final List<Network> networks;

    public TrustedProxyMatcher(List<String> trustedProxies) {
        List<Network> parsed = new ArrayList<>();
        if (trustedProxies != null) {
            trustedProxies.forEach(value -> parsed.add(parse(value)));
        }
        this.networks = List.copyOf(parsed);
    }

    public boolean matches(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        byte[] candidate = addressBytes(address.trim());
        return networks.stream().anyMatch(network -> network.matches(candidate));
    }

    private static Network parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("trusted proxy must not be blank");
        }
        String[] parts = value.trim().split("/", -1);
        byte[] address = addressBytes(parts[0]);
        int bits = address.length * Byte.SIZE;
        int prefix = parts.length == 1 ? bits : parsePrefix(parts[1], bits);
        return new Network(address, prefix);
    }

    private static int parsePrefix(String value, int max) {
        try {
            int prefix = Integer.parseInt(value);
            if (prefix < 0 || prefix > max) {
                throw new IllegalArgumentException("trusted proxy prefix is out of range");
            }
            return prefix;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("trusted proxy prefix is invalid", exception);
        }
    }

    private static byte[] addressBytes(String value) {
        if (!value.matches("[0-9a-fA-F:.]+")) {
            throw new IllegalArgumentException("trusted proxy must be an IP address or CIDR");
        }
        try {
            return InetAddress.getByName(value).getAddress();
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("trusted proxy address is invalid", exception);
        }
    }

    private record Network(byte[] address, int prefix) {

        private boolean matches(byte[] candidate) {
            if (candidate.length != address.length) {
                return false;
            }
            int completeBytes = prefix / Byte.SIZE;
            int remainingBits = prefix % Byte.SIZE;
            for (int index = 0; index < completeBytes; index++) {
                if (candidate[index] != address[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xff << (Byte.SIZE - remainingBits);
            return (candidate[completeBytes] & mask) == (address[completeBytes] & mask);
        }
    }
}
