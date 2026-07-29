package top.egon.cola.component.accessguard.key.contributor;

import top.egon.cola.component.accessguard.core.GuardInvocation;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.key.GuardKeyPart;
import top.egon.cola.component.accessguard.key.GuardKeyResolutionException;
import top.egon.cola.component.accessguard.key.TrustedProxyMatcher;

import java.util.List;

public final class ClientIpKeyContributor implements GuardKeyContributor {

    public static final String HTTP_REQUEST_ATTRIBUTE = "accessGuard.httpRequest";

    private final TrustedProxyMatcher trustedProxyMatcher;

    public ClientIpKeyContributor(TrustedProxyMatcher trustedProxyMatcher) {
        this.trustedProxyMatcher = trustedProxyMatcher;
    }

    @Override
    public String id() {
        return "CLIENT_IP";
    }

    @Override
    public List<GuardKeyPart> contribute(GuardInvocation invocation, KeyConfig config) {
        Object candidate = invocation.attributes().get(HTTP_REQUEST_ATTRIBUTE);
        String remote = HttpRequestAccess.remoteAddress(candidate);
        String client = remote;
        if (trustedProxyMatcher.matches(remote)) {
            String forwarded = forwardedAddress(HttpRequestAccess.header(candidate, "Forwarded"));
            if (forwarded == null) {
                forwarded = firstAddress(HttpRequestAccess.header(candidate, "X-Forwarded-For"));
            }
            if (forwarded != null) {
                client = forwarded;
            }
        }
        if (client == null || client.isBlank()) {
            throw new GuardKeyResolutionException("CLIENT_IP_MISSING");
        }
        return List.of(new GuardKeyPart("ip", client, 0));
    }

    private static String forwardedAddress(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String first = header.split(",", 2)[0];
        for (String parameter : first.split(";")) {
            String trimmed = parameter.trim();
            if (trimmed.regionMatches(true, 0, "for=", 0, 4)) {
                return cleanAddress(trimmed.substring(4));
            }
        }
        return null;
    }

    private static String firstAddress(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        return cleanAddress(header.split(",", 2)[0]);
    }

    private static String cleanAddress(String value) {
        String cleaned = value.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.startsWith("[")) {
            int closing = cleaned.indexOf(']');
            return closing > 0 ? cleaned.substring(1, closing) : cleaned;
        }
        int colon = cleaned.indexOf(':');
        if (colon > 0 && cleaned.indexOf(':', colon + 1) < 0) {
            return cleaned.substring(0, colon);
        }
        return cleaned;
    }
}
