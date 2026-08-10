package top.egon.cola.platform.idp.admin.identity.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringPasswordHashAdapterTest {

    private final SpringPasswordHashAdapter adapter =
            new SpringPasswordHashAdapter();

    @Test
    void encodesNewPasswordsWithArgon2AndVerifiesThem() {
        char[] password = "strong-password-1".toCharArray();

        String encoded = adapter.encode(password);

        assertTrue(encoded.startsWith("{argon2}"));
        assertTrue(adapter.matches(
                "strong-password-1".toCharArray(),
                encoded
        ));
        assertFalse(adapter.needsUpgrade(encoded));
    }

    @Test
    void verifiesLegacyBcryptAndRequestsUpgrade() {
        String legacy = "{bcrypt}" + new BCryptPasswordEncoder().encode(
                "strong-password-1"
        );

        assertTrue(adapter.matches(
                "strong-password-1".toCharArray(),
                legacy
        ));
        assertTrue(adapter.needsUpgrade(legacy));
    }
}
