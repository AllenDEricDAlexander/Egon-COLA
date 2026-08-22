package top.egon.cola.platform.idp.admin.oauth.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientSecretEntity;

import java.util.Optional;

/** Repository for hash-only Client Secret credentials. */
public interface IdentityClientSecretRepository
        extends JpaRepository<IdentityClientSecretEntity, String> {

    /** Finds a Client Secret by its safe lifecycle metadata. */
    Optional<IdentityClientSecretEntity> findByClientIdAndStatus(
            String clientId,
            IdentityClientSecretEntity.Status status
    );

    /**
     * Loads the active Secret with a database write lock.
     *
     * <p>Loads the active Secret with a database write lock for atomic rotation.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select secret from IdentityClientSecretEntity secret "
            + "where secret.clientId = :clientId and secret.status = :status")
    Optional<IdentityClientSecretEntity> findByClientIdAndStatusForUpdate(
            @Param("clientId") String clientId,
            @Param("status") IdentityClientSecretEntity.Status status
    );

    /** Finds the active Secret without exposing plaintext. */
    default Optional<IdentityClientSecretEntity> findActiveByClientId(
            String clientId
    ) {
        return findByClientIdAndStatus(
                clientId,
                IdentityClientSecretEntity.Status.ACTIVE
        );
    }

    /** Finds and locks the active Secret for rotation. */
    default Optional<IdentityClientSecretEntity> findActiveByClientIdForUpdate(
            String clientId
    ) {
        return findByClientIdAndStatusForUpdate(
                clientId,
                IdentityClientSecretEntity.Status.ACTIVE
        );
    }
}
