package top.egon.cola.platform.tianquan.shoubing.admin.token.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;

@Validated
public interface OAuthSecurityStateRepository {

    Optional<OAuthSecurityStateBO> findByTarget(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull OAuthSecurityTargetEnum targetType,
            @NotBlank @Size(max = 128) String targetKey);

    Optional<OAuthSecurityStateBO> findByTargetForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull OAuthSecurityTargetEnum targetType,
            @NotBlank @Size(max = 128) String targetKey);

    List<OAuthSecurityStateBO> findByRevisionAfter(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @PositiveOrZero long afterRevision,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit);

    OAuthSecurityStateBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthSecurityStateBO state,
            @PositiveOrZero long expectedVersion);
}
