package top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSigningKeyMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSigningKeyConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthSigningKeyPO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;

@Repository("oAuthSigningKeyRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthSigningKeyRepositoryImpl extends EgonColaRepository<OAuthSigningKeyMapper, OAuthSigningKeyPO>
        implements OAuthSigningKeyRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";
    private static final Set<String> PUBLIC_JWK_MEMBERS = Set.of("kty", "kid", "alg", "use", "n", "e");

    @Getter
    @Qualifier("OAuthSigningKeyMapper")
    private final OAuthSigningKeyMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthSigningKeyConverterImpl")
    private final OAuthSigningKeyConverter converter;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthSigningKeyBO> findByKid(
            TrustedRealmBO trustedRealm,
            String kid) {
        String value = requiredKid(kid);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectByKid(tenantId, value))
                .map(converter::toTarget));
    }

    @Override
    @Transactional
    public Optional<OAuthSigningKeyBO> findByKidForUpdate(
            TrustedRealmBO trustedRealm,
            String kid) {
        String value = requiredKid(kid);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectByKidForUpdate(tenantId, value))
                .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthSigningKeyBO> findByStatuses(
            TrustedRealmBO trustedRealm,
            Collection<OAuthKeyStatusEnum> statuses,
            long afterId,
            int limit) {
        List<OAuthKeyStatusEnum> values = List.copyOf(Objects.requireNonNull(statuses, "statuses"));
        if (values.isEmpty() || values.size() > 5 || limit < 1 || limit > 8) {
            throw new IllegalArgumentException("OAUTH_SIGNING_KEY_QUERY_RANGE_INVALID");
        }
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectByStatuses(tenantId, values, afterId, limit).stream()
                .map(converter::toTarget)
                .toList());
    }

    @Override
    @Transactional
    public OAuthSigningKeyBO save(
            TrustedRealmBO trustedRealm,
            OAuthSigningKeyBO key,
            long expectedVersion) {
        Objects.requireNonNull(key, "key");
        String kid = requiredKid(key.getKid());
        validateSigningKey(key);
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            try {
                OAuthSigningKeyPO current = baseMapper.selectByKidForUpdate(tenantId, kid);
                OAuthSigningKeyPO value = converter.toSource(key);
                value.setTenantId(tenantId);
                if (current == null) {
                    if (key.getId() != null || expectedVersion != 0L || key.getVersion() != 0L) {
                        throw stale(expectedVersion, -1L);
                    }
                    value.setVersion(0L);
                    if (!super.save(value)) {
                        throw new IllegalStateException("OAUTH_SIGNING_KEY_INSERT_NOT_APPLIED");
                    }
                    return converter.toTarget(value);
                }
                long nextVersion = Math.addExact(expectedVersion, 1L);
                if (current.getVersion() != expectedVersion || key.getVersion() != nextVersion
                        || !Objects.equals(key.getId(), current.getId())
                        || !Objects.equals(key.getKid(), current.getKid())
                        || !Objects.equals(key.getAlgorithm(), current.getAlgorithm())
                        || !Objects.equals(key.getPublishedAt(), current.getPublishedAt())
                        || !Objects.equals(key.getPrivateKeyCiphertext(), current.getPrivateKeyCiphertext())
                        || !Objects.equals(key.getPublicJwk(), current.getPublicJwk())) {
                    throw stale(expectedVersion, current.getVersion());
                }
                value.setId(current.getId());
                value.setTenantId(tenantId);
                value.setVersion(nextVersion);
                value.setUpdateUserId(userIdProvider.currentUserId());
                value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
                if (baseMapper.updateExpectedVersion(tenantId, value, expectedVersion) != 1) {
                    throw stale(expectedVersion, current.getVersion());
                }
                return converter.toTarget(value);
            } catch (DataIntegrityViolationException failure) {
                throw new IllegalStateException("OAUTH_SIGNING_KEY_CONFLICT", failure);
            }
        });
    }

    @Override
    @Transactional
    public boolean deleteVersionedById(
            TrustedRealmBO trustedRealm,
            Long id,
            long expectedVersion) {
        Objects.requireNonNull(id, "id");
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            OAuthSigningKeyPO current = baseMapper.selectActiveById(tenantId, id);
            if (current == null || current.getVersion() != expectedVersion
                    || current.getStatus() != OAuthKeyStatusEnum.RETIRED
                    || current.getVerifyUntil() == null
                    || !clock.instant().isAfter(current.getVerifyUntil())) {
                throw new IllegalStateException("OAUTH_SIGNING_KEY_RETENTION_REQUIRED");
            }
            OAuthSigningKeyPO value = new OAuthSigningKeyPO();
            value.setId(id);
            value.setTenantId(tenantId);
            value.setVersion(expectedVersion);
            value.setUpdateUserId(userIdProvider.currentUserId());
            value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
            return baseMapper.deleteVersionedById(tenantId, value, expectedVersion) == 1;
        });
    }

    private <T> T withTrustedStoragePartition(
            TrustedRealmBO trustedRealm,
            Function<Long, T> operation) {
        Objects.requireNonNull(trustedRealm, "trustedRealm").validateTrustedPartition();
        Objects.requireNonNull(operation, "operation");
        String tenantKey = properties.getTenantId().getMdcKey();
        String userKey = properties.getAudit().getUserIdMdcKey();
        if (tenantKey.equals(userKey)) {
            throw new IllegalStateException("OAUTH_SIGNING_KEY_MDC_KEYS_INVALID");
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
                throw new IllegalStateException("OAUTH_SIGNING_KEY_TENANT_CONTEXT_MISMATCH");
            }
            return operation.apply(tenantId);
        } finally {
            restoreMdc(tenantKey, previousTenant);
            restoreMdc(userKey, previousUser);
        }
    }

    private static String requiredKid(String kid) {
        if (kid == null || !kid.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("OAUTH_SIGNING_KEY_KID_INVALID");
        }
        return kid;
    }

    private static void validateSigningKey(OAuthSigningKeyBO key) {
        if (!"RS256".equals(key.getAlgorithm()) || key.getStatus() == null || key.getPublishedAt() == null) {
            throw new IllegalArgumentException("OAUTH_SIGNING_KEY_FIELDS_INVALID");
        }
        Objects.requireNonNull(key.getPublicJwk(), "publicJwk");
        if (!key.getPublicJwk().isObject()) {
            throw new IllegalArgumentException("OAUTH_SIGNING_PUBLIC_JWK_INVALID");
        }
        Set<String> members = new java.util.HashSet<>();
        key.getPublicJwk().fieldNames().forEachRemaining(members::add);
        if (!PUBLIC_JWK_MEMBERS.equals(members)
                || !"RSA".equals(key.getPublicJwk().path("kty").asText())
                || !Objects.equals(key.getKid(), key.getPublicJwk().path("kid").asText())
                || !"RS256".equals(key.getPublicJwk().path("alg").asText())
                || !"sig".equals(key.getPublicJwk().path("use").asText())) {
            throw new IllegalArgumentException("OAUTH_SIGNING_PUBLIC_JWK_INVALID");
        }
        String modulus = key.getPublicJwk().path("n").asText();
        String exponent = key.getPublicJwk().path("e").asText();
        try {
            BigInteger modulusValue = new BigInteger(1, Base64.getUrlDecoder().decode(modulus));
            BigInteger exponentValue = new BigInteger(1, Base64.getUrlDecoder().decode(exponent));
            if (modulusValue.bitLength() < 2_048 || exponentValue.compareTo(BigInteger.valueOf(3L)) < 0
                    || !exponentValue.testBit(0) || exponentValue.compareTo(modulusValue) >= 0) {
                throw new IllegalArgumentException("OAUTH_SIGNING_PUBLIC_JWK_INVALID");
            }
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("OAUTH_SIGNING_PUBLIC_JWK_INVALID", failure);
        }
        String encryptedKey = key.getPrivateKeyCiphertext();
        if (encryptedKey == null
                || !encryptedKey.matches(
                "ENC:v1:[A-Za-z0-9][A-Za-z0-9._-]{0,63}:[A-Za-z0-9_-]{16}:[A-Za-z0-9_-]{22,}")
                || encryptedKey.length() > 32_768) {
            throw new IllegalArgumentException("OAUTH_SIGNING_PRIVATE_KEY_CIPHERTEXT_INVALID");
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
                "signing key expected version " + expectedVersion + " but was " + actualVersion);
    }
}
