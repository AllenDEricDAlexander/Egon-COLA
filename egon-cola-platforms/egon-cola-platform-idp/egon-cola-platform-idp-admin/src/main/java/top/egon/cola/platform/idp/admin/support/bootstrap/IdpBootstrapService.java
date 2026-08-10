package top.egon.cola.platform.idp.admin.support.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.identity.PasswordCredential;
import top.egon.cola.platform.idp.core.identity.UsernameNormalizer;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

@Service
public class IdpBootstrapService implements IdpBootstrapRunner.BootstrapPort {

    private final IdentityUserStore users;
    private final PasswordCredentialStore credentials;
    private final PasswordHashPort passwordHashes;
    private final LongIdGenerator ids;
    private final UsernameNormalizer usernameNormalizer;
    private final Clock clock;

    @Autowired
    public IdpBootstrapService(
            IdentityUserStore users,
            PasswordCredentialStore credentials,
            PasswordHashPort passwordHashes,
            LongIdGenerator ids
    ) {
        this(
                users,
                credentials,
                passwordHashes,
                ids,
                new UsernameNormalizer(),
                Clock.systemUTC()
        );
    }

    IdpBootstrapService(
            IdentityUserStore users,
            PasswordCredentialStore credentials,
            PasswordHashPort passwordHashes,
            LongIdGenerator ids,
            UsernameNormalizer usernameNormalizer,
            Clock clock
    ) {
        this.users = Objects.requireNonNull(users, "users");
        this.credentials = Objects.requireNonNull(credentials, "credentials");
        this.passwordHashes = Objects.requireNonNull(
                passwordHashes,
                "passwordHashes"
        );
        this.ids = Objects.requireNonNull(ids, "ids");
        this.usernameNormalizer = Objects.requireNonNull(
                usernameNormalizer,
                "usernameNormalizer"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public void bootstrap(String username, char[] password) {
        Objects.requireNonNull(password, "password");
        try {
            String normalizedUsername = usernameNormalizer.normalize(username);
            if (users.findByNormalizedUsername(normalizedUsername).isPresent()) {
                throw new IllegalStateException(
                        "bootstrap identity already exists"
                );
            }
            Instant now = clock.instant();
            String identitySub = ids.nextId();
            String displayUsername = requiredUsername(username);
            users.save(new IdentityUser(
                    identitySub,
                    displayUsername,
                    normalizedUsername,
                    displayUsername,
                    IdentityUserStatus.ACTIVE,
                    0L,
                    0,
                    null,
                    null,
                    0L
            ), 0L);
            credentials.save(new PasswordCredential(
                    identitySub,
                    passwordHashes.encode(password),
                    now,
                    false,
                    PasswordCredential.Status.ACTIVE,
                    0L
            ), 0L);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private String requiredUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        return username.trim();
    }
}
