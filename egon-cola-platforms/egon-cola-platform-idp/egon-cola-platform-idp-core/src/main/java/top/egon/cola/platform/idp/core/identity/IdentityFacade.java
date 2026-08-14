package top.egon.cola.platform.idp.core.identity;

import top.egon.cola.platform.idp.contract.IdpErrorCode;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 用户凭据与当前身份操作门面。
 * Facade for user credentials and current-identity operations.
 */
public final class IdentityFacade {

    private static final String UNKNOWN_IDENTITY = "UNKNOWN";

    private final IdentityUserStore userStore;
    private final PasswordCredentialStore credentialStore;
    private final PasswordHashPort passwordHash;
    private final IdentityUserStatePort userState;
    private final IdentitySecurityEventPort securityEvents;
    private final RefreshTokenStore refreshTokens;
    private final UsernameNormalizer usernameNormalizer;
    private final IntSupplier maximumFailures;
    private final Supplier<Duration> lockDuration;

    public IdentityFacade(
            IdentityUserStore userStore,
            PasswordCredentialStore credentialStore,
            PasswordHashPort passwordHash,
            IdentityUserStatePort userState,
            IdentitySecurityEventPort securityEvents,
            RefreshTokenStore refreshTokens,
            UsernameNormalizer usernameNormalizer,
            int maximumFailures,
            Duration lockDuration
    ) {
        this(
                userStore, credentialStore, passwordHash, userState,
                securityEvents, refreshTokens, usernameNormalizer,
                () -> maximumFailures, () -> lockDuration);
        currentMaximumFailures();
        currentLockDuration();
    }

    public static IdentityFacade dynamicPolicy(
            IdentityUserStore userStore,
            PasswordCredentialStore credentialStore,
            PasswordHashPort passwordHash,
            IdentityUserStatePort userState,
            IdentitySecurityEventPort securityEvents,
            RefreshTokenStore refreshTokens,
            UsernameNormalizer usernameNormalizer,
            IntSupplier maximumFailures,
            Supplier<Duration> lockDuration
    ) {
        return new IdentityFacade(
                userStore, credentialStore, passwordHash, userState,
                securityEvents, refreshTokens, usernameNormalizer,
                maximumFailures, lockDuration);
    }

    private IdentityFacade(
            IdentityUserStore userStore,
            PasswordCredentialStore credentialStore,
            PasswordHashPort passwordHash,
            IdentityUserStatePort userState,
            IdentitySecurityEventPort securityEvents,
            RefreshTokenStore refreshTokens,
            UsernameNormalizer usernameNormalizer,
            IntSupplier maximumFailures,
            Supplier<Duration> lockDuration
    ) {
        this.userStore = Objects.requireNonNull(userStore, "userStore");
        this.credentialStore = Objects.requireNonNull(credentialStore, "credentialStore");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.userState = Objects.requireNonNull(userState, "userState");
        this.securityEvents = Objects.requireNonNull(securityEvents, "securityEvents");
        this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
        this.usernameNormalizer = Objects.requireNonNull(usernameNormalizer, "usernameNormalizer");
        this.maximumFailures = Objects.requireNonNull(maximumFailures, "maximumFailures");
        this.lockDuration = Objects.requireNonNull(lockDuration, "lockDuration");
    }

    public AuthenticatedIdentity authenticate(
            String username,
            char[] rawPassword,
            String sourceBucket,
            Instant now
    ) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        Objects.requireNonNull(now, "now");
        String source = required(sourceBucket, "sourceBucket");
        try {
            String normalizedUsername = usernameNormalizer.normalize(username);
            IdentityUser user = userStore.findByNormalizedUsername(normalizedUsername)
                    .orElse(null);
            if (user == null) {
                passwordHash.matches(rawPassword, passwordHash.dummyHash());
                record("IDENTITY_LOGIN_FAILED", UNKNOWN_IDENTITY,
                        "INVALID_CREDENTIALS", source, now);
                throw invalidCredentials();
            }
            return authenticateKnownUser(user, rawPassword, source, now);
        } finally {
            Arrays.fill(rawPassword, '\0');
        }
    }

    /**
     * Re-authenticates only the already authenticated subject for step-up.
     * Re-authentication never trusts a caller-supplied username or tenant.
     */
    public AuthenticatedIdentity authenticateCurrent(
            String identitySub,
            char[] rawPassword,
            Instant now
    ) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        Objects.requireNonNull(now, "now");
        String subject = required(identitySub, "identitySub");
        try {
            IdentityUser user = userStore.findById(subject)
                    .orElseThrow(this::invalidCredentials);
            return authenticateKnownUser(user, rawPassword, "STEP_UP", now);
        } finally {
            Arrays.fill(rawPassword, '\0');
        }
    }

    private AuthenticatedIdentity authenticateKnownUser(
            IdentityUser sourceUser,
            char[] rawPassword,
            String source,
            Instant now
    ) {
        IdentityUser user = requireLoginAllowed(sourceUser, now);
        PasswordCredential credential = credentialStore.findActive(user.id()).orElse(null);
        String encoded = credential == null
                ? passwordHash.dummyHash() : credential.passwordHash();
        if (!passwordHash.matches(rawPassword, encoded) || credential == null) {
            IdentityUser failed = user.failedAt(
                    now, currentMaximumFailures(), currentLockDuration());
            userStore.save(failed, user.version());
            record("IDENTITY_LOGIN_FAILED", user.id(),
                    "INVALID_CREDENTIALS", source, now);
            throw invalidCredentials();
        }
        if (passwordHash.needsUpgrade(credential.passwordHash())) {
            long currentCredentialVersion = credential.version();
            credential = credential.rehashed(passwordHash.encode(rawPassword));
            credentialStore.save(credential, currentCredentialVersion);
        }
        IdentityUser authenticated = user.authenticatedAt(now);
        userStore.save(authenticated, user.version());
        userState.publish(new IdentityUserState(
                authenticated.id(),
                IdentityUserState.Status.valueOf(authenticated.status().name()),
                now));
        record("IDENTITY_LOGIN_SUCCEEDED", user.id(), "AUTHENTICATED", source, now);
        return new AuthenticatedIdentity(
                authenticated.id(), authenticated.username(),
                authenticated.displayName(), credential.mustChangePassword());
    }

    private int currentMaximumFailures() {
        int value = maximumFailures.getAsInt();
        if (value < 1) {
            throw new IllegalStateException("maximumFailures policy must be positive");
        }
        return value;
    }

    private Duration currentLockDuration() {
        Duration value = Objects.requireNonNull(lockDuration.get(), "lockDuration policy");
        if (value.isNegative() || value.isZero()) {
            throw new IllegalStateException("lockDuration policy must be positive");
        }
        return value;
    }

    public void changePassword(
            String identitySub,
            char[] oldPassword,
            char[] newPassword,
            Instant now
    ) {
        Objects.requireNonNull(oldPassword, "oldPassword");
        Objects.requireNonNull(newPassword, "newPassword");
        Objects.requireNonNull(now, "now");
        try {
            IdentityUser user = userStore.findById(identitySub)
                    .orElseThrow(this::invalidCredentials);
            PasswordCredential credential = credentialStore.findActive(user.id())
                    .orElseThrow(this::invalidCredentials);
            if (!passwordHash.matches(oldPassword, credential.passwordHash())) {
                throw invalidCredentials();
            }
            validateNewPassword(oldPassword, newPassword);
            PasswordCredential changed = credential.changed(
                    passwordHash.encode(newPassword), now);
            credentialStore.save(changed, credential.version());
            IdentityUser changedUser = user.revokeSecurityState();
            userStore.save(changedUser, user.version());
            refreshTokens.revokeSubject(changedUser.id(), "PASSWORD_CHANGED", now);
            publishSecurityState(changedUser, "PASSWORD_CHANGED", now);
        } finally {
            Arrays.fill(oldPassword, '\0');
            Arrays.fill(newPassword, '\0');
        }
    }

    private IdentityUser requireLoginAllowed(IdentityUser user, Instant now) {
        if (user.status() == IdentityUserStatus.DISABLED) {
            throw new IdentityException(IdpErrorCode.USER_DISABLED, "authentication failed");
        }
        if (user.status() == IdentityUserStatus.LOCKED
                && user.lockedUntil().isAfter(now)) {
            throw new IdentityException(IdpErrorCode.USER_LOCKED, "authentication failed");
        }
        IdentityUser unlocked = user.unlockIfExpired(now);
        return unlocked == user ? user : userStore.save(unlocked, user.version());
    }

    private void validateNewPassword(char[] oldPassword, char[] newPassword) {
        if (newPassword.length < 12) {
            throw new IllegalArgumentException("new password must contain at least 12 characters");
        }
        if (Arrays.equals(oldPassword, newPassword)) {
            throw new IllegalArgumentException("new password must differ from the old password");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char character : newPassword) {
            hasLetter |= Character.isLetter(character);
            hasDigit |= Character.isDigit(character);
        }
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("new password must contain a letter and a digit");
        }
    }

    private void publishSecurityState(IdentityUser user, String reason, Instant now) {
        userState.publish(new IdentityUserState(
                user.id(), IdentityUserState.Status.valueOf(user.status().name()), now));
        record("IDENTITY_TOKEN_REVOKED", user.id(), reason, "SELF_SERVICE", now);
    }

    private void record(
            String eventType,
            String identitySub,
            String reason,
            String sourceBucket,
            Instant now
    ) {
        securityEvents.append(new IdentitySecurityEvent(
                eventType, identitySub, reason, sourceBucket, now));
    }

    private IdentityException invalidCredentials() {
        return new IdentityException(IdpErrorCode.INVALID_CREDENTIALS, "authentication failed");
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
