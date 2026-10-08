package top.egon.cola.platform.tianquan.shoubing.admin.identity.repo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repository.IdentityCredentialRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repository.IdentityUserRepository;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;
import top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore;

import java.util.Objects;
import java.util.Optional;

@Repository("identityPersistenceAdapter")
@Slf4j
@RequiredArgsConstructor
@Validated
public class IdentityPersistenceAdapter implements IdentityUserStore, PasswordCredentialStore {

    @Qualifier("identityUserRepository")
    private final IdentityUserRepository users;
    @Qualifier("identityCredentialRepository")
    private final IdentityCredentialRepository credentials;

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityUser> findByNormalizedUsername(
            @NotBlank @Size(max = 128) String normalizedUsername) {
        return users.findByNormalizedUsername(required(normalizedUsername, "normalizedUsername"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityUser> findById(@NotBlank @Size(max = 64) String identitySub) {
        return users.findByIdentitySub(required(identitySub, "identitySub"));
    }

    @Override
    @Transactional
    public IdentityUser save(@NotNull IdentityUser user, @PositiveOrZero long expectedVersion) {
        return users.save(Objects.requireNonNull(user, "user"), expectedVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordCredential> findActive(@NotBlank @Size(max = 64) String identitySub) {
        return credentials.findByIdentitySub(required(identitySub, "identitySub"))
                .filter(credential -> credential.status() == PasswordCredential.Status.ACTIVE);
    }

    @Override
    @Transactional
    public PasswordCredential save(
            @NotNull PasswordCredential credential,
            @PositiveOrZero long expectedVersion) {
        return credentials.save(Objects.requireNonNull(credential, "credential"), expectedVersion);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
