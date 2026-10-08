package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;

import java.util.List;
import java.util.Optional;

@Validated
public interface OAuthClientRepository {

    Optional<OAuthClientBO> findById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id);

    Optional<OAuthClientBO> findByClientId(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Size(max = 128) String clientId);

    Optional<OAuthClientBO> findByClientIdForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Size(max = 128) String clientId);

    List<OAuthClientBO> findAllByRealm(@NotNull @Valid TrustedRealmBO trustedRealm);

    OAuthClientBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthClientBO client,
            @PositiveOrZero long expectedVersion);
}
