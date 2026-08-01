package top.egon.cola.platform.rbac3.admin.auth.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Local password authenticator with an enumeration-safe failure contract.
 */
public final class PasswordIdentityAuthenticator implements IdentityAuthenticatorStrategy {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final int MAX_BCRYPT_BYTES = 72;

    private final CredentialStore credentialStore;
    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public PasswordIdentityAuthenticator(
            CredentialStore credentialStore,
            PasswordEncoder passwordEncoder) {
        this.credentialStore = Objects.requireNonNull(credentialStore, "credentialStore");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.dummyHash = passwordEncoder.encode("rbac3-enumeration-resistant-dummy-password");
    }

    @Override
    public AuthenticatedIdentity authenticate(LoginRequest request, Instant now) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(now, "now");
        String tenantCode = normalize(request.tenantCode());
        String username = normalize(request.username());
        return credentialStore.withCredential(tenantCode, username,
                credential -> authenticateLocked(credential, request.password(), now));
    }

    private AuthenticatedIdentity authenticateLocked(
            PasswordCredential credential,
            String rawPassword,
            Instant now) {
        if (credential == null) {
            passwordEncoder.matches(safePassword(rawPassword), dummyHash);
            throw failed();
        }
        if (!credential.active()
                || credential.lockedUntil() != null && now.isBefore(credential.lockedUntil())) {
            passwordEncoder.matches(safePassword(rawPassword), dummyHash);
            throw failed();
        }
        boolean overlong = rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES;
        boolean passwordMatches = passwordEncoder.matches(
                safePassword(rawPassword), credential.passwordHash());
        if (overlong || !passwordMatches) {
            int failureCount = credential.failureCount() + 1;
            Instant lockedUntil = failureCount >= MAX_FAILURES
                    ? now.plus(LOCK_DURATION)
                    : null;
            credentialStore.save(credential.failed(failureCount, lockedUntil));
            throw failed();
        }
        credentialStore.save(credential.succeeded());
        if (passwordEncoder.upgradeEncoding(credential.passwordHash())) {
            credentialStore.updatePasswordHash(
                    credential,
                    passwordEncoder.encode(rawPassword),
                    now);
        }
        return new AuthenticatedIdentity(
                credential.tenantCode(),
                credential.userId(),
                "PASSWORD",
                1);
    }

    private static String safePassword(String rawPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_BYTES
                ? rawPassword
                : "rbac3-overlong-password-dummy";
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
    }

    private static AuthenticationFailed failed() {
        return new AuthenticationFailed();
    }

    public interface CredentialStore {

        <T> T withCredential(
                String tenantCode,
                String normalizedUsername,
                Function<PasswordCredential, T> action);

        void save(PasswordCredential credential);

        default void updatePasswordHash(
                PasswordCredential credential,
                String passwordHash,
                Instant changedAt) {
        }
    }

    public record PasswordCredential(
            String tenantCode,
            String normalizedUsername,
            String userId,
            String passwordHash,
            int failureCount,
            Instant lockedUntil,
            boolean active
    ) {

        public PasswordCredential {
            Objects.requireNonNull(tenantCode, "tenantCode");
            Objects.requireNonNull(normalizedUsername, "normalizedUsername");
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(passwordHash, "passwordHash");
            if (failureCount < 0) {
                throw new IllegalArgumentException("failureCount must not be negative");
            }
        }

        PasswordCredential failed(int newFailureCount, Instant newLockedUntil) {
            return new PasswordCredential(
                    tenantCode,
                    normalizedUsername,
                    userId,
                    passwordHash,
                    newFailureCount,
                    newLockedUntil,
                    active);
        }

        PasswordCredential succeeded() {
            return new PasswordCredential(
                    tenantCode,
                    normalizedUsername,
                    userId,
                    passwordHash,
                    0,
                    null,
                    active);
        }

        @Override
        public String toString() {
            return "PasswordCredential[tenantCode=" + tenantCode
                    + ", normalizedUsername=" + normalizedUsername
                    + ", userId=" + userId
                    + ", passwordHash=<redacted>, failureCount=" + failureCount
                    + ", lockedUntil=" + lockedUntil
                    + ", active=" + active + ']';
        }
    }

    public static final class AuthenticationFailed extends RuntimeException {

        private static final String REASON_CODE = "AUTHENTICATION_FAILED";

        private AuthenticationFailed() {
            super(REASON_CODE);
        }

        public String reasonCode() {
            return REASON_CODE;
        }
    }
}
