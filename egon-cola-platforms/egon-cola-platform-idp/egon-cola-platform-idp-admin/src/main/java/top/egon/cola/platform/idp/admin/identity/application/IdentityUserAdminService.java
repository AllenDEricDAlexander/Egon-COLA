package top.egon.cola.platform.idp.admin.identity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.identity.PasswordCredential;
import top.egon.cola.platform.idp.core.identity.UsernameNormalizer;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class IdentityUserAdminService {

    private final IdentityUserStore users;
    private final PasswordCredentialStore credentials;
    private final UserDirectory directory;
    private final PasswordHashPort passwordHashes;
    private final IdentityUserStatePort states;
    private final IdentitySecurityEventPort securityEvents;
    private final LongIdGenerator ids;
    private final UsernameNormalizer normalizer;
    private final Clock clock;
    private final Supplier<String> temporaryPasswords;

    public IdentityUserAdminService(
            IdentityUserStore users,
            PasswordCredentialStore credentials,
            UserDirectory directory,
            PasswordHashPort passwordHashes,
            IdentityUserStatePort states,
            IdentitySecurityEventPort securityEvents,
            LongIdGenerator ids
    ) {
        this(
                users,
                credentials,
                directory,
                passwordHashes,
                states,
                securityEvents,
                ids,
                new UsernameNormalizer(),
                Clock.systemUTC(),
                secureTemporaryPasswordGenerator()
        );
    }

    IdentityUserAdminService(
            IdentityUserStore users,
            PasswordCredentialStore credentials,
            UserDirectory directory,
            PasswordHashPort passwordHashes,
            IdentityUserStatePort states,
            IdentitySecurityEventPort securityEvents,
            LongIdGenerator ids,
            UsernameNormalizer normalizer,
            Clock clock,
            Supplier<String> temporaryPasswords
    ) {
        this.users = Objects.requireNonNull(users, "users");
        this.credentials = Objects.requireNonNull(credentials, "credentials");
        this.directory = Objects.requireNonNull(directory, "directory");
        this.passwordHashes = Objects.requireNonNull(
                passwordHashes,
                "passwordHashes"
        );
        this.states = Objects.requireNonNull(states, "states");
        this.securityEvents = Objects.requireNonNull(
                securityEvents,
                "securityEvents"
        );
        this.ids = Objects.requireNonNull(ids, "ids");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.temporaryPasswords = Objects.requireNonNull(
                temporaryPasswords,
                "temporaryPasswords"
        );
    }

    @Transactional(readOnly = true)
    public List<UserView> list() {
        return directory.list().stream()
                .sorted(Comparator.comparing(IdentityUser::normalizedUsername))
                .map(IdentityUserAdminService::view)
                .toList();
    }

    @Transactional
    public CreatedUserView create(CreateUserCommand command) {
        Objects.requireNonNull(command, "command");
        String normalized = normalizer.normalize(command.username());
        if (users.findByNormalizedUsername(normalized).isPresent()) {
            throw new IllegalStateException("identity username already exists");
        }
        Instant now = clock.instant();
        String subject = ids.nextId();
        String username = trimmed(command.username(), "username");
        IdentityUser user = new IdentityUser(
                subject,
                username,
                normalized,
                required(command.displayName(), "displayName"),
                IdentityUserStatus.ACTIVE,
                0L,
                0,
                null,
                null,
                0L
        );
        String oneTimePassword = temporaryPassword();
        char[] rawPassword = oneTimePassword.toCharArray();
        try {
            users.save(user, 0L);
            credentials.save(new PasswordCredential(
                    subject,
                    passwordHashes.encode(rawPassword),
                    now,
                    true,
                    PasswordCredential.Status.ACTIVE,
                    0L
            ), 0L);
        } finally {
            Arrays.fill(rawPassword, '\0');
        }
        publish(user, "IDENTITY_USER_CREATED", "USER_CREATED", now);
        return new CreatedUserView(
                subject,
                username,
                user.displayName(),
                user.status().name(),
                oneTimePassword
        );
    }

    @Transactional
    public UserView update(String identitySub, UpdateUserCommand command) {
        Objects.requireNonNull(command, "command");
        IdentityUser current = user(identitySub);
        if (current.version() != command.expectedVersion()) {
            throw new IllegalStateException("stale identity version");
        }
        IdentityUserStatus status = Objects.requireNonNull(
                command.status(),
                "status"
        );
        boolean revoke = current.status() != status
                && status == IdentityUserStatus.DISABLED;
        IdentityUser updated = current.administrativelyUpdated(
                command.displayName(),
                status,
                revoke
        );
        users.save(updated, current.version());
        if (revoke) {
            publish(
                    updated,
                    "IDENTITY_USER_DISABLED",
                    "USER_DISABLED",
                    clock.instant()
            );
        } else {
            states.publish(state(updated, clock.instant()));
        }
        return view(updated);
    }

    @Transactional
    public ResetPasswordView resetPassword(String identitySub) {
        IdentityUser current = user(identitySub);
        PasswordCredential credential = credentials.findActive(current.id())
                .orElseThrow(() -> new IllegalStateException(
                        "active password credential was not found"
                ));
        String oneTimePassword = temporaryPassword();
        char[] rawPassword = oneTimePassword.toCharArray();
        Instant now = clock.instant();
        try {
            credentials.save(
                    credential.reset(passwordHashes.encode(rawPassword), now),
                    credential.version()
            );
        } finally {
            Arrays.fill(rawPassword, '\0');
        }
        IdentityUser revoked = current.revokeSecurityState();
        users.save(revoked, current.version());
        publish(
                revoked,
                "IDENTITY_PASSWORD_RESET",
                "PASSWORD_RESET",
                now
        );
        return new ResetPasswordView(
                revoked.id(),
                oneTimePassword,
                true,
                revoked.tokenVersion()
        );
    }

    @Transactional
    public UserView revokeAll(String identitySub) {
        IdentityUser current = user(identitySub);
        IdentityUser revoked = current.revokeSecurityState();
        users.save(revoked, current.version());
        publish(
                revoked,
                "IDENTITY_TOKEN_REVOKED",
                "ADMIN_REVOKE_ALL",
                clock.instant()
        );
        return view(revoked);
    }

    private void publish(
            IdentityUser user,
            String eventType,
            String reason,
            Instant now
    ) {
        states.publish(state(user, now));
        if (user.tokenVersion() > 0L) {
            states.revokeFamilies(user.id(), user.tokenVersion(), reason);
        }
        securityEvents.append(new IdentitySecurityEvent(
                eventType,
                user.id(),
                reason,
                "ADMIN_API",
                user.tokenVersion(),
                now
        ));
    }

    private IdentityUser user(String identitySub) {
        return users.findById(required(identitySub, "identitySub"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "identity user was not found"
                ));
    }

    private String temporaryPassword() {
        String value = temporaryPasswords.get();
        if (value == null
                || value.length() < 16
                || value.length() > 128
                || !value.matches(".*[A-Za-z].*")
                || !value.matches(".*[0-9].*")) {
            throw new IllegalStateException(
                    "temporary password generator returned an invalid value"
            );
        }
        return value;
    }

    private static IdentityUserState state(IdentityUser user, Instant now) {
        return new IdentityUserState(
                user.id(),
                IdentityUserState.Status.valueOf(user.status().name()),
                user.tokenVersion(),
                now
        );
    }

    private static UserView view(IdentityUser user) {
        return new UserView(
                user.id(),
                user.username(),
                user.displayName(),
                user.status().name(),
                user.tokenVersion(),
                user.failedLoginCount(),
                user.lockedUntil(),
                user.lastLoginAt(),
                user.version()
        );
    }

    private static Supplier<String> secureTemporaryPasswordGenerator() {
        SecureRandom random = new SecureRandom();
        return () -> {
            byte[] value = new byte[18];
            random.nextBytes(value);
            return "T9a" + Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(value);
        };
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String trimmed(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    @FunctionalInterface
    public interface UserDirectory {
        List<IdentityUser> list();
    }

    public record CreateUserCommand(String username, String displayName) {
    }

    public record UpdateUserCommand(
            String displayName,
            IdentityUserStatus status,
            long expectedVersion
    ) {
    }

    public record UserView(
            String subject,
            String username,
            String displayName,
            String status,
            long tokenVersion,
            int failedLoginCount,
            Instant lockedUntil,
            Instant lastLoginAt,
            long version
    ) {
    }

    public record CreatedUserView(
            String subject,
            String username,
            String displayName,
            String status,
            String oneTimePassword
    ) {
    }

    public record ResetPasswordView(
            String subject,
            String oneTimePassword,
            boolean mustChangePassword,
            long tokenVersion
    ) {
    }
}
