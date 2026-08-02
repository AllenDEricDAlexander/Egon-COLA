package top.egon.cola.platform.idp.core.identity;

import java.text.Normalizer;
import java.util.Locale;

public final class UsernameNormalizer {

    public String normalize(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username is required");
        }
        String normalized = Normalizer.normalize(
                username,
                Normalizer.Form.NFKC
        ).trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        return normalized;
    }
}
