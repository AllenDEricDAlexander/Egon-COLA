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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;

@Validated
public interface OAuthSigningKeyRepository {

    Optional<OAuthSigningKeyBO> findByKid(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") @Size(max = 128) String kid);

    Optional<OAuthSigningKeyBO> findByKidForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") @Size(max = 128) String kid);

    List<OAuthSigningKeyBO> findByStatuses(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Size(min = 1, max = 5) Collection<@NotNull OAuthKeyStatusEnum> statuses,
            @PositiveOrZero long afterId,
            @Min(1) @Max(8) int limit);

    OAuthSigningKeyBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthSigningKeyBO key,
            @PositiveOrZero long expectedVersion);

    boolean deleteVersionedById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id,
            @PositiveOrZero long expectedVersion);
}
