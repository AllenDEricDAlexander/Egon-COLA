package top.egon.cola.platform.idp.core.identity;

import top.egon.cola.platform.idp.contract.IdpErrorCode;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public final class IdentityFacade {

    private static final String UNKNOWN_IDENTITY = "UNKNOWN";

    private final IdentityUserStore userStore;
    private final PasswordCredentialStore credentialStore;
    private final PasswordHashPort passwordHash;
    private final IdentityUserStatePort userState;
    private final IdentitySecurityEventPort securityEvents;
    private final UsernameNormalizer usernameNormalizer;
    private final int maximumFailures;
    private final Duration lockDuration;

    public IdentityFacade(
            IdentityUserStore userStore,
            PasswordCredentialStore credentialStore,
            PasswordHashPort passwordHash,
            IdentityUserStatePort userState,
            IdentitySecurityEventPort securityEvents,
            UsernameNormalizer usernameNormalizer,
            int maximumFailures,
            Duration lockDuration
    ) {
        this.userStore = Objects.requireNonNull(userStore, "userStore");
        this.credentialStore = Objects.requireNonNull(
                credentialStore,
                "credentialStore"
        );
        this.passwordHash = Objects.requireNonNull(
                passwordHash,
                "passwordHash"
        );
        this.userState = Objects.requireNonNull(userState, "userState");
        this.securityEvents = Objects.requireNonNull(
                securityEvents,
                "securityEvents"
        );
        this.usernameNormalizer = Objects.requireNonNull(
                usernameNormalizer,
                "usernameNormalizer"
        );
        if (maximumFailures < 1) {
            throw new IllegalArgumentException(
                    "maximumFailures must be positive"
            );
        }
        this.maximumFailures = maximumFailures;
        this.lockDuration = Objects.requireNonNull(
                lockDuration,
                "lockDuration"
        );
        if (lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException(
                    "lockDuration must be positive"
            );
        }
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
            IdentityUser user = userStore.findByNormalizedUsername(
                    normalizedUsername
            ).orElse(null);
            if (user == null) {
                passwordHash.matches(rawPassword, passwordHash.dummyHash());
                record(
                        "IDENTITY_LOGIN_FAILED",
                        UNKNOWN_IDENTITY,
                        "INVALID_CREDENTIALS",
                        source,
                        0L,
                        now
                );
                throw invalidCredentials();
            }
            user = requireLoginAllowed(user, now);
            PasswordCredential credential = credentialStore.findActive(
                    user.id()
            ).orElse(null);
            String encoded = credential == null
                    ? passwordHash.dummyHash()
                    : credential.passwordHash();
            if (!passwordHash.matches(rawPassword, encoded)
                    || credential == null) {
                IdentityUser failed = user.failedAt(
                        now,
                        maximumFailures,
                        lockDuration
                );
                userStore.save(failed, user.version());
                record(
                        "IDENTITY_LOGIN_FAILED",
                        user.id(),
                        "INVALID_CREDENTIALS",
                        source,
                        user.tokenVersion(),
                        now
                );
                throw invalidCredentials();
            }
            if (passwordHash.needsUpgrade(credential.passwordHash())) {
                PasswordCredential upgraded = credential.rehashed(
                        passwordHash.encode(rawPassword)
                );
                credentialStore.save(upgraded, credential.version());
                credential = upgraded;
            }
            IdentityUser authenticated = user.authenticatedAt(now);
            userStore.save(authenticated, user.version());
            record(
                    "IDENTITY_LOGIN_SUCCEEDED",
                    user.id(),
                    "AUTHENTICATED",
                    source,
                    user.tokenVersion(),
                    now
            );
            return new AuthenticatedIdentity(
                    authenticated.id(),
                    authenticated.username(),
                    authenticated.displayName(),
                    authenticated.tokenVersion(),
                    credential.mustChangePassword()
            );
        } finally {
            Arrays.fill(rawPassword, '\0');
        }
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
            PasswordCredential credential = credentialStore.findActive(
                    user.id()
            ).orElseThrow(this::invalidCredentials);
            if (!passwordHash.matches(
                    oldPassword,
                    credential.passwordHash()
            )) {
                throw invalidCredentials();
            }
            validateNewPassword(oldPassword, newPassword);
            PasswordCredential changedCredential = credential.changed(
                    passwordHash.encode(newPassword),
                    now
            );
            credentialStore.save(changedCredential, credential.version());
            IdentityUser changedUser = user.revokeSecurityState();
            userStore.save(changedUser, user.version());
            publishSecurityState(
                    changedUser,
                    "PASSWORD_CHANGED",
                    now
            );
        } finally {
            Arrays.fill(oldPassword, '\0');
            Arrays.fill(newPassword, '\0');
        }
    }

    private IdentityUser requireLoginAllowed(
            IdentityUser user,
            Instant now
    ) {
        if (user.status() == IdentityUserStatus.DISABLED) {
            throw new IdentityException(
                    IdpErrorCode.USER_DISABLED,
                    "authentication failed"
            );
        }
        if (user.status() == IdentityUserStatus.LOCKED
                && user.lockedUntil().isAfter(now)) {
            throw new IdentityException(
                    IdpErrorCode.USER_LOCKED,
                    "authentication failed"
            );
        }
        IdentityUser unlocked = user.unlockIfExpired(now);
        if (unlocked != user) {
            return userStore.save(unlocked, user.version());
        }
        return user;
    }

    private void validateNewPassword(
            char[] oldPassword,
            char[] newPassword
    ) {
        if (newPassword.length < 12) {
            throw new IllegalArgumentException(
                    "new password must contain at least 12 characters"
            );
        }
        if (Arrays.equals(oldPassword, newPassword)) {
            throw new IllegalArgumentException(
                    "new password must differ from the old password"
            );
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char character : newPassword) {
            hasLetter |= Character.isLetter(character);
            hasDigit |= Character.isDigit(character);
        }
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException(
                    "new password must contain a letter and a digit"
            );
        }
    }

    private void publishSecurityState(
            IdentityUser user,
            String reason,
            Instant now
    ) {
        userState.publish(new IdentityUserState(
                user.id(),
                IdentityUserState.Status.valueOf(user.status().name()),
                user.tokenVersion(),
                now
        ));
        userState.revokeFamilies(
                user.id(),
                user.tokenVersion(),
                reason
        );
        record(
                "IDENTITY_TOKEN_REVOKED",
                user.id(),
                reason,
                "SELF_SERVICE",
                user.tokenVersion(),
                now
        );
    }

    private void record(
            String eventType,
            String identitySub,
            String reason,
            String sourceBucket,
            long tokenVersion,
            Instant now
    ) {
        securityEvents.append(new IdentitySecurityEvent(
                eventType,
                identitySub,
                reason,
                sourceBucket,
                tokenVersion,
                now
        ));
    }

    private IdentityException invalidCredentials() {
        return new IdentityException(
                IdpErrorCode.INVALID_CREDENTIALS,
                "authentication failed"
        );
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
