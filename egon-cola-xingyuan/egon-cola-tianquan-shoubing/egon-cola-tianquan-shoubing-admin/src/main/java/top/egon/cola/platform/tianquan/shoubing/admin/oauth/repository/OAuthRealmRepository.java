package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;

import java.util.List;
import java.util.Optional;

@Validated
public interface OAuthRealmRepository {

    Optional<OAuthRealmBO> findByIssuer(@NotNull @Valid TrustedRealmBO trustedRealm);

    Optional<OAuthRealmBO> findByRealmKey(@NotNull @Valid TrustedRealmBO trustedRealm);

    Optional<OAuthRealmBO> findByRealmKeyForUpdate(@NotNull @Valid TrustedRealmBO trustedRealm);

    List<OAuthRealmBO> findAllByStoragePartition(@NotNull @Valid TrustedRealmBO trustedRealm);

    OAuthRealmBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthRealmBO realm,
            @PositiveOrZero long expectedVersion);
}
