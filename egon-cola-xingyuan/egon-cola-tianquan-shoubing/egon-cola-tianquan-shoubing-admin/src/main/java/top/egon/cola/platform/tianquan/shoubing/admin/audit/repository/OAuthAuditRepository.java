package top.egon.cola.platform.tianquan.shoubing.admin.audit.repository;

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
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.dto.IdentityAuditQueryDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;

@Validated
public interface OAuthAuditRepository {

    Optional<OAuthAuditBO> findByEventId(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Size(max = 64) String eventId);

    List<OAuthAuditBO> findByOccurredAt(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull Instant fromInclusive,
            @NotNull Instant toExclusive,
            Instant afterOccurredAt,
            @PositiveOrZero Long afterId,
            @Min(1) @Max(500) int limit);

    List<OAuthAuditBO> findExpiredBefore(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull Instant cutoff,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit);

    List<OAuthAuditBO> findIdentityPage(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotNull @Valid IdentityAuditQueryDTO query);

    long countIdentityPage(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotNull @Valid IdentityAuditQueryDTO query);

    OAuthAuditBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthAuditBO audit,
            @PositiveOrZero long expectedVersion);

    boolean deleteVersionedById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Positive Long id,
            @PositiveOrZero long expectedVersion);
}
