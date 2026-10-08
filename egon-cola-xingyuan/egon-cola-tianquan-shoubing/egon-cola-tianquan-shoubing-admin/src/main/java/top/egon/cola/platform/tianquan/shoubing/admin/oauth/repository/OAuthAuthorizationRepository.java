package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository;

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
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;

@Validated
public interface OAuthAuthorizationRepository {

    Optional<OAuthAuthorizationBO> findById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id);

    Optional<OAuthAuthorizationBO> findByIdForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id);

    List<OAuthAuthorizationBO> findByPrincipalName(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Size(max = 128) String principalName,
            @NotBlank @Pattern(regexp = "ACTIVE|REVOKED|EXPIRED") String status,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit);

    List<OAuthAuthorizationBO> findExpiredBefore(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull Instant expiresBefore,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit);

    OAuthAuthorizationBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthAuthorizationBO authorization,
            @PositiveOrZero long expectedVersion);

    boolean deleteVersionedById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id,
            @PositiveOrZero long expectedVersion);
}
