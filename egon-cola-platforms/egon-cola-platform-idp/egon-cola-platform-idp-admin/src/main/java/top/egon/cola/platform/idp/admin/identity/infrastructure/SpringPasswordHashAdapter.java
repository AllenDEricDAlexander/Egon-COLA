package top.egon.cola.platform.idp.admin.identity.infrastructure;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

import java.nio.CharBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public final class SpringPasswordHashAdapter implements PasswordHashPort {

    private static final String ARGON2_ID = "argon2";

    private final DelegatingPasswordEncoder delegate;
    private final String dummyHash;

    public SpringPasswordHashAdapter() {
        Map<String, PasswordEncoder> encoders = new LinkedHashMap<>();
        encoders.put(
                ARGON2_ID,
                Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        );
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        delegate = new DelegatingPasswordEncoder(ARGON2_ID, encoders);
        dummyHash = delegate.encode("idp-dummy-password-1");
    }

    @Override
    public boolean matches(char[] rawPassword, String encodedPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        return delegate.matches(
                CharBuffer.wrap(rawPassword),
                Objects.requireNonNull(encodedPassword, "encodedPassword")
        );
    }

    @Override
    public String encode(char[] rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        return delegate.encode(CharBuffer.wrap(rawPassword));
    }

    @Override
    public String dummyHash() {
        return dummyHash;
    }

    @Override
    public boolean needsUpgrade(String encodedPassword) {
        return delegate.upgradeEncoding(
                Objects.requireNonNull(encodedPassword, "encodedPassword")
        );
    }
}
