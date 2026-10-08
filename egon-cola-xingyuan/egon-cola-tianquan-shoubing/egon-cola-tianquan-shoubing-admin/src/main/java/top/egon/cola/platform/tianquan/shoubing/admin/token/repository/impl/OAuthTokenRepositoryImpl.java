package top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthTokenMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthTokenConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthTokenPO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent;

@Repository("oAuthTokenRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthTokenRepositoryImpl extends EgonColaRepository<OAuthTokenMapper, OAuthTokenPO>
        implements OAuthTokenRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";

    @Getter
    @Qualifier("OAuthTokenMapper")
    private final OAuthTokenMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthTokenConverterImpl")
    private final OAuthTokenConverter converter;
    @Qualifier("oauthPayloadCipherComponent")
    private final OAuthPayloadCipherComponent payloadCipher;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthTokenBO> findByTokenDigest(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String tokenDigest) {
        String digest = requiredDigest(tokenDigest);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectByTokenDigest(tenantId, digest))
                .map(value -> toTarget(trustedRealm, digest, value)));
    }

    @Override
    @Transactional
    public Optional<OAuthTokenBO> findByTokenDigestForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Pattern(regexp = "[0-9a-f]{64}") String tokenDigest) {
        String digest = requiredDigest(tokenDigest);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectByTokenDigestForUpdate(tenantId, digest))
                .map(value -> toTarget(trustedRealm, digest, value)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthTokenBO> findByAuthorization(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long authorizationPk,
            @NotNull OAuthTokenTypeEnum tokenType,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit) {
        Objects.requireNonNull(authorizationPk, "authorizationPk");
        Objects.requireNonNull(tokenType, "tokenType");
        requireLimit(limit);
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectByAuthorization(tenantId, authorizationPk, tokenType, afterId, limit).stream()
                .map(value -> toTarget(trustedRealm, value.getTokenDigest(), value))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthTokenBO> findExpiredBefore(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull Instant expiresBefore,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit) {
        Objects.requireNonNull(expiresBefore, "expiresBefore");
        requireLimit(limit);
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectExpired(tenantId, expiresBefore, afterId, limit).stream()
                .map(value -> toTarget(trustedRealm, value.getTokenDigest(), value))
                .toList());
    }

    @Override
    @Transactional
    public OAuthTokenBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthTokenBO token,
            @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(token, "token");
        String digest = requiredDigest(token.getTokenDigest());
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            try {
                OAuthTokenPO current = baseMapper.selectByTokenDigestForUpdate(tenantId, digest);
                if (current == null) {
                    if (token.getId() != null || expectedVersion != 0L || token.getVersion() != 0L) {
                        throw stale(expectedVersion, -1L);
                    }
                    OAuthTokenPO value = converter.toSource(
                            trustedRealm, payloadCipher, objectMapper, digest, token);
                    value.setTenantId(tenantId);
                    value.setVersion(0L);
                    if (!super.save(value)) {
                        throw new IllegalStateException("OAUTH_TOKEN_INSERT_NOT_APPLIED");
                    }
                    return toTarget(trustedRealm, digest, value);
                }
                long nextVersion = Math.addExact(expectedVersion, 1L);
                if (current.getVersion() != expectedVersion || token.getVersion() != nextVersion
                        || !Objects.equals(token.getId(), current.getId())
                        || !Objects.equals(token.getAuthorizationPk(), current.getAuthorizationPk())
                        || token.getTokenType() != current.getTokenType()
                        || !Objects.equals(token.getJti(), current.getJti())
                        || !Objects.equals(token.getIssuedAt(), current.getIssuedAt())
                        || !Objects.equals(token.getExpiresAt(), current.getExpiresAt())) {
                    throw stale(expectedVersion, current.getVersion());
                }
                OAuthTokenPO value = converter.toSource(trustedRealm, payloadCipher, objectMapper, digest, token);
                value.setId(current.getId());
                value.setTenantId(tenantId);
                value.setTokenCiphertext(current.getTokenCiphertext());
                value.setVersion(nextVersion);
                value.setUpdateUserId(userIdProvider.currentUserId());
                value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
                if (baseMapper.updateExpectedVersion(tenantId, value, expectedVersion) != 1) {
                    throw stale(expectedVersion, current.getVersion());
                }
                return toTarget(trustedRealm, digest, value);
            } catch (DataIntegrityViolationException failure) {
                throw new IllegalStateException("OAUTH_TOKEN_CONFLICT", failure);
            }
        });
    }

    @Override
    @Transactional
    public boolean deleteVersionedById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id,
            @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(id, "id");
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            OAuthTokenPO value = new OAuthTokenPO();
            value.setId(id);
            value.setTenantId(tenantId);
            value.setVersion(expectedVersion);
            value.setUpdateUserId(userIdProvider.currentUserId());
            value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
            return baseMapper.deleteVersionedById(tenantId, value, expectedVersion) == 1;
        });
    }

    private OAuthTokenBO toTarget(TrustedRealmBO trustedRealm, String digest, OAuthTokenPO value) {
        return converter.toTarget(trustedRealm, payloadCipher, objectMapper, digest, value);
    }

    private <T> T withTrustedStoragePartition(
            TrustedRealmBO trustedRealm,
            Function<Long, T> operation) {
        Objects.requireNonNull(trustedRealm, "trustedRealm").validateTrustedPartition();
        Objects.requireNonNull(operation, "operation");
        String tenantKey = properties.getTenantId().getMdcKey();
        String userKey = properties.getAudit().getUserIdMdcKey();
        if (tenantKey.equals(userKey)) {
            throw new IllegalStateException("OAUTH_TOKEN_MDC_KEYS_INVALID");
        }
        String previousTenant = MDC.get(tenantKey);
        String previousUser = MDC.get(userKey);
        try {
            MDC.put(tenantKey, Long.toString(trustedRealm.getStoragePartitionId()));
            if (previousUser == null || previousUser.isBlank()) {
                MDC.put(userKey, SERVICE_ACTOR);
            }
            Long tenantId = EgonColaTenantIdProvider.currentTenantId();
            if (!tenantId.equals(trustedRealm.getStoragePartitionId())) {
                throw new IllegalStateException("OAUTH_TOKEN_TENANT_CONTEXT_MISMATCH");
            }
            return operation.apply(tenantId);
        } finally {
            restoreMdc(tenantKey, previousTenant);
            restoreMdc(userKey, previousUser);
        }
    }

    private static String requiredDigest(String tokenDigest) {
        if (tokenDigest == null || !tokenDigest.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("OAUTH_TOKEN_DIGEST_INVALID");
        }
        return tokenDigest;
    }

    private static void requireLimit(int limit) {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("OAUTH_TOKEN_QUERY_LIMIT_INVALID");
        }
    }

    private static void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    private static OptimisticLockingFailureException stale(long expectedVersion, long actualVersion) {
        return new OptimisticLockingFailureException(
                "token expected version " + expectedVersion + " but was " + actualVersion);
    }
}
