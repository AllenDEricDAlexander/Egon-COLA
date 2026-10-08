package top.egon.cola.platform.tianquan.shoubing.admin.token.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;

@Validated
public interface OAuthRevocationRepository {

    Optional<OAuthRevocationBO> findByTarget(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull OAuthSecurityTargetEnum targetType,
            @NotBlank @Size(max = 128) String targetKey);

    Optional<OAuthRevocationBO> findByTargetForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull OAuthSecurityTargetEnum targetType,
            @NotBlank @Size(max = 128) String targetKey);

    List<OAuthRevocationBO> findByProjectionStatus(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Size(max = 16) String projectionStatus,
            @PositiveOrZero long afterRevision,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit);

    List<OAuthRevocationBO> findExpiredBefore(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull Instant cutoff,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit);

    OAuthRevocationBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthRevocationBO revocation,
            @PositiveOrZero long expectedVersion);

    boolean deleteVersionedById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Positive Long id,
            @PositiveOrZero long expectedVersion);
}
