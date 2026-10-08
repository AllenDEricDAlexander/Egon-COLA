package top.egon.cola.platform.tianquan.shoubing.admin.token.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;

@Validated
public interface OAuthTokenRepository {

    Optional<OAuthTokenBO> findByTokenDigest(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String tokenDigest);

    Optional<OAuthTokenBO> findByTokenDigestForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String tokenDigest);

    List<OAuthTokenBO> findByAuthorization(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long authorizationPk,
            @NotNull OAuthTokenTypeEnum tokenType,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit);

    List<OAuthTokenBO> findExpiredBefore(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull Instant expiresBefore,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit);

    OAuthTokenBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthTokenBO token,
            @PositiveOrZero long expectedVersion);

    boolean deleteVersionedById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id,
            @PositiveOrZero long expectedVersion);
}
