package top.egon.cola.component.accessguard.core.plan;

import java.util.List;

public record KeyConfig(
        List<String> contributors,
        List<String> trustedProxies,
        String hmacSecret,
        List<String> headers,
        int maxPartLength
) {

    public KeyConfig {
        contributors = contributors == null ? List.of() : List.copyOf(contributors);
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
        hmacSecret = hmacSecret == null ? "" : hmacSecret;
        headers = headers == null ? List.of() : List.copyOf(headers);
        if (maxPartLength <= 0) {
            throw new IllegalArgumentException("maxPartLength must be positive");
        }
    }

    public KeyConfig(List<String> contributors, List<String> trustedProxies, String hmacSecret) {
        this(contributors, trustedProxies, hmacSecret, List.of(), 1024);
    }

    @Override
    public String toString() {
        return "KeyConfig[contributors=" + contributors
                + ", trustedProxies=" + trustedProxies
                + ", hmacSecret=<redacted>, headers=" + headers
                + ", maxPartLength=" + maxPartLength + "]";
    }
}
