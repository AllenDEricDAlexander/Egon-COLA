package top.egon.cola.component.accessguard.core.plan;

import java.util.List;

public record KeyConfig(
        List<String> contributors,
        List<String> trustedProxies,
        String hmacSecret
) {

    public KeyConfig {
        contributors = contributors == null ? List.of() : List.copyOf(contributors);
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
        hmacSecret = hmacSecret == null ? "" : hmacSecret;
    }

    @Override
    public String toString() {
        return "KeyConfig[contributors=" + contributors
                + ", trustedProxies=" + trustedProxies
                + ", hmacSecret=<redacted>]";
    }
}
