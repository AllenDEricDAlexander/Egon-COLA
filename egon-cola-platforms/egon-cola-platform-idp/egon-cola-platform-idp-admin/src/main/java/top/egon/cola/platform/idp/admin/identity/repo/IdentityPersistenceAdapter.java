package top.egon.cola.platform.idp.admin.identity.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.idp.admin.identity.domain.pojo.IdentityCredentialEntity;
import top.egon.cola.platform.idp.admin.identity.domain.pojo.IdentityUserEntity;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.PasswordCredential;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Repository
public class IdentityPersistenceAdapter
        implements IdentityUserStore, PasswordCredentialStore {

    private final EntityManager entityManager;
    private final Clock clock;

    @Autowired
    public IdentityPersistenceAdapter(EntityManager entityManager) {
        this(entityManager, Clock.systemUTC());
    }

    IdentityPersistenceAdapter(EntityManager entityManager, Clock clock) {
        this.entityManager = Objects.requireNonNull(
                entityManager,
                "entityManager"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityUser> findByNormalizedUsername(
            String normalizedUsername
    ) {
        return entityManager.createQuery("""
                        select u
                          from IdentityUserEntity u
                         where u.normalizedUsername = :username
                        """, IdentityUserEntity.class)
                .setParameter(
                        "username",
                        required(normalizedUsername, "normalizedUsername")
                )
                .getResultStream()
                .findFirst()
                .map(IdentityUserEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityUser> findById(String identitySub) {
        return Optional.ofNullable(entityManager.find(
                IdentityUserEntity.class,
                required(identitySub, "identitySub")
        )).map(IdentityUserEntity::toDomain);
    }

    @Override
    @Transactional
    public IdentityUser save(IdentityUser user, long expectedVersion) {
        Objects.requireNonNull(user, "user");
        Instant now = clock.instant();
        IdentityUserEntity entity = entityManager.find(
                IdentityUserEntity.class,
                user.id(),
                LockModeType.PESSIMISTIC_WRITE
        );
        if (entity == null) {
            requireNewVersion(user.id(), user.version(), expectedVersion);
            entityManager.persist(IdentityUserEntity.fromDomain(user, now));
            entityManager.flush();
            return user;
        }
        requireExpectedVersion(
                user.id(),
                entity.getVersion(),
                expectedVersion,
                user.version()
        );
        entity.apply(user, now);
        entityManager.flush();
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordCredential> findActive(String identitySub) {
        IdentityCredentialEntity entity = entityManager.find(
                IdentityCredentialEntity.class,
                required(identitySub, "identitySub")
        );
        if (entity == null) {
            return Optional.empty();
        }
        PasswordCredential credential = entity.toDomain();
        if (credential.status() != PasswordCredential.Status.ACTIVE) {
            return Optional.empty();
        }
        return Optional.of(credential);
    }

    @Override
    @Transactional
    public PasswordCredential save(
            PasswordCredential credential,
            long expectedVersion
    ) {
        Objects.requireNonNull(credential, "credential");
        Instant now = clock.instant();
        IdentityCredentialEntity entity = entityManager.find(
                IdentityCredentialEntity.class,
                credential.identitySub(),
                LockModeType.PESSIMISTIC_WRITE
        );
        if (entity == null) {
            requireNewVersion(
                    credential.identitySub(),
                    credential.version(),
                    expectedVersion
            );
            entityManager.persist(IdentityCredentialEntity.fromDomain(
                    credential,
                    now
            ));
            entityManager.flush();
            return credential;
        }
        requireExpectedVersion(
                credential.identitySub(),
                entity.getVersion(),
                expectedVersion,
                credential.version()
        );
        entity.apply(credential, now);
        entityManager.flush();
        return credential;
    }

    private void requireNewVersion(
            String identity,
            long newVersion,
            long expectedVersion
    ) {
        if (expectedVersion != 0L || newVersion != 0L) {
            throw stale(identity, expectedVersion, -1L);
        }
    }

    private void requireExpectedVersion(
            String identity,
            long actualVersion,
            long expectedVersion,
            long newVersion
    ) {
        if (actualVersion != expectedVersion
                || newVersion != Math.addExact(expectedVersion, 1L)) {
            throw stale(identity, expectedVersion, actualVersion);
        }
    }

    private OptimisticLockingFailureException stale(
            String identity,
            long expectedVersion,
            long actualVersion
    ) {
        return new OptimisticLockingFailureException(
                "identity " + identity + " expected version "
                        + expectedVersion + " but was " + actualVersion
        );
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
