package top.egon.cola.platform.tianquan.shoubing.admin.resource.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;

import java.util.List;
import java.util.Optional;

@Validated
public interface OAuthGrantRepository {

    Optional<OAuthGrantBO> findGrant(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @NotBlank @Size(max = 128) String resourceServerId,
            @NotNull ResourceGrantType grantType);

    Optional<OAuthGrantBO> findGrantForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @NotBlank @Size(max = 128) String resourceServerId,
            @NotNull ResourceGrantType grantType);

    List<OAuthGrantBO> listByClient(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @Min(1) @Max(65) int limit);

    OAuthGrantBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthGrantBO grant,
            @PositiveOrZero long expectedVersion);
}
