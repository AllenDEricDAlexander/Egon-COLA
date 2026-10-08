package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;

@Validated
public interface OAuthConsentRepository {

    Optional<OAuthConsentBO> findConsent(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @NotBlank @Size(max = 128) String subject);

    Optional<OAuthConsentBO> findConsentForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @NotBlank @Size(max = 128) String subject);

    OAuthConsentBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthConsentBO consent,
            @PositiveOrZero long expectedVersion);

    boolean deleteVersionedById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id,
            @PositiveOrZero long expectedVersion);
}
