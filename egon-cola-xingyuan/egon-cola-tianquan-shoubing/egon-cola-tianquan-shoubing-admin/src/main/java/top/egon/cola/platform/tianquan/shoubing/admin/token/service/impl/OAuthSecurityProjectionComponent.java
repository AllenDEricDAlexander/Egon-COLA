package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthRevocationProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthRevocationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSecurityStateRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository;
import top.egon.cola.platform.tianquan.shoubing.core.audit.OAuthSecurityChangedEvent;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

@Component("oauthSecurityProjectionComponent")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthSecurityProjectionComponent {

    private static final int RECOVERY_PAGE_SIZE = 500;
    private static final Instant MAX_AUTHORIZATION_CUTOFF = Instant.parse("9999-12-31T23:59:59Z");
    private static final String REDIS_PREFIX = "egon:oauth:security:v2:{%s}:";
    private static final String RECOVERY_LOCK_PREFIX = "egon:oauth:security:recovery:{%s}";
    private static final String SNAPSHOT_SCRIPT = """
            local readyEpoch = redis.call('HGET', KEYS[1], 'epoch') or ''
            local readyRevision = redis.call('HGET', KEYS[1], 'lastRevision') or ''
            local readyStatus = redis.call('HGET', KEYS[1], 'status') or ''
            local realmGeneration = redis.call('HGET', KEYS[2], 'generation') or ''
            local realmEnabled = redis.call('HGET', KEYS[2], 'enabled') or ''
            local realmRevision = redis.call('HGET', KEYS[2], 'revision') or ''
            local clientGeneration = redis.call('HGET', KEYS[3], 'generation') or ''
            local clientEnabled = redis.call('HGET', KEYS[3], 'enabled') or ''
            local clientRevision = redis.call('HGET', KEYS[3], 'revision') or ''
            local result = {readyEpoch, readyRevision, readyStatus, realmGeneration,
                    realmEnabled, realmRevision, clientGeneration, clientEnabled, clientRevision}
            if #KEYS == 4 then
              table.insert(result, redis.call('HGET', KEYS[4], 'generation') or '')
              table.insert(result, redis.call('HGET', KEYS[4], 'enabled') or '')
              table.insert(result, redis.call('HGET', KEYS[4], 'revision') or '')
            end
            return result
            """;
    private static final String BEGIN_RECOVERY_SCRIPT = """
            redis.call('HSET', KEYS[1], 'epoch', ARGV[1], 'lastRevision', '0', 'status', 'RECOVERING')
            return 1
            """;
    private static final String APPLY_PROJECTION_SCRIPT = """
            local configuredEpoch = ARGV[1]
            local projectionKind = ARGV[2]
            local revision = tonumber(ARGV[3])
            local generation = ARGV[4]
            local enabled = ARGV[5]
            local expiresAt = tonumber(ARGV[6]) or 0
            local reasonCode = ARGV[7]
            local targetType = ARGV[8]
            local readyEpoch = redis.call('HGET', KEYS[1], 'epoch') or ''
            local readyStatus = redis.call('HGET', KEYS[1], 'status') or ''
            if readyEpoch ~= configuredEpoch or readyStatus ~= 'READY' then
              return -1
            end
            local currentRevision = tonumber(redis.call('HGET', KEYS[2], 'revision') or '-1')
            if revision < currentRevision then
              return 0
            end
            if projectionKind == 'GENERATION' then
              if revision == currentRevision then
                if (redis.call('HGET', KEYS[2], 'targetType') or '') == targetType
                    and (redis.call('HGET', KEYS[2], 'generation') or '') == generation
                    and (redis.call('HGET', KEYS[2], 'enabled') or '') == enabled then
                  return 1
                end
                return -2
              end
              redis.call('HSET', KEYS[2], 'targetType', targetType, 'generation', generation,
                  'enabled', enabled, 'revision', tostring(revision))
              redis.call('PERSIST', KEYS[2])
            else
              if expiresAt > 0 and expiresAt <= tonumber(ARGV[9]) then
                redis.call('DEL', KEYS[2])
              else
                if revision == currentRevision then
                  if (redis.call('HGET', KEYS[2], 'targetType') or '') == targetType
                      and (redis.call('HGET', KEYS[2], 'expiresAt') or '0') == tostring(expiresAt)
                      and (redis.call('HGET', KEYS[2], 'reasonCode') or '') == reasonCode then
                    return 1
                  end
                  return -2
                end
                redis.call('HSET', KEYS[2], 'targetType', targetType, 'expiresAt', tostring(expiresAt),
                    'reasonCode', reasonCode, 'revision', tostring(revision))
                if expiresAt > 0 then
                  redis.call('PEXPIREAT', KEYS[2], expiresAt)
                else
                  redis.call('PERSIST', KEYS[2])
                end
              end
            end
            local readyRevision = tonumber(redis.call('HGET', KEYS[1], 'lastRevision') or '0')
            if revision > readyRevision then
              redis.call('HSET', KEYS[1], 'lastRevision', tostring(revision))
            end
            return 2
            """;
    private static final String RECOVERY_WRITE_SCRIPT = """
            local projectionKind = ARGV[1]
            local targetType = ARGV[2]
            local revision = tonumber(ARGV[3])
            local generation = ARGV[4]
            local enabled = ARGV[5]
            local expiresAt = tonumber(ARGV[6]) or 0
            local reasonCode = ARGV[7]
            local currentRevision = tonumber(redis.call('HGET', KEYS[1], 'revision') or '-1')
            if revision < currentRevision then
              return 0
            end
            if projectionKind == 'GENERATION' then
              if revision == currentRevision then
                if (redis.call('HGET', KEYS[1], 'targetType') or '') == targetType
                    and (redis.call('HGET', KEYS[1], 'generation') or '') == generation
                    and (redis.call('HGET', KEYS[1], 'enabled') or '') == enabled then
                  return 1
                end
                return -1
              end
              redis.call('HSET', KEYS[1], 'targetType', targetType, 'generation', generation,
                  'enabled', enabled, 'revision', tostring(revision))
              return 2
            end
            if revision == currentRevision then
              if (redis.call('HGET', KEYS[1], 'targetType') or '') == targetType
                  and (redis.call('HGET', KEYS[1], 'expiresAt') or '0') == tostring(expiresAt)
                  and (redis.call('HGET', KEYS[1], 'reasonCode') or '') == reasonCode then
                return 1
              end
              return -1
            end
            redis.call('HSET', KEYS[1], 'targetType', targetType, 'expiresAt', tostring(expiresAt),
                'reasonCode', reasonCode, 'revision', tostring(revision))
            if expiresAt > 0 then
              redis.call('PEXPIREAT', KEYS[1], expiresAt)
            end
            return 2
            """;
    private static final String COMPLETE_RECOVERY_SCRIPT = """
            if (redis.call('HGET', KEYS[1], 'epoch') or '') ~= ARGV[1]
                or (redis.call('HGET', KEYS[1], 'status') or '') ~= 'RECOVERING' then
              return 0
            end
            redis.call('HSET', KEYS[1], 'lastRevision', ARGV[2], 'status', 'READY')
            return 1
            """;

    @Qualifier("rbac3RuntimeRedissonClient")
    private final RedissonClient redisson;
    @Qualifier("oAuthSecurityStateRepository")
    private final OAuthSecurityStateRepository stateRepository;
    @Qualifier("oAuthRevocationRepository")
    private final OAuthRevocationRepository revocationRepository;
    @Qualifier("oAuthTokenRepository")
    private final OAuthTokenRepository tokenRepository;
    @Qualifier("oAuthAuthorizationRepository")
    private final OAuthAuthorizationRepository authorizationRepository;
    @Qualifier("oAuthClientRepository")
    private final OAuthClientRepository clientRepository;
    @Qualifier("oAuthRealmRepository")
    private final OAuthRealmRepository realmRepository;
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver managementRealmResolver;
    @Qualifier("oauthRevocationProperties")
    private final OAuthRevocationProperties revocationProperties;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @PostConstruct
    void validatePrimaryConfiguration() {
        revocationProperties.validateWith(redisson);
    }

    public void applyProjection(
            TrustedRealmBO trustedRealm,
            OAuthSecurityChangedEvent event) {
        TrustedRealmBO realm = Objects.requireNonNull(trustedRealm, "trustedRealm");
        realm.validateTrustedPartition();
        OAuthSecurityChangedEvent changedEvent = Objects.requireNonNull(event, "event");
        requireMatchingRealm(realm, changedEvent);
        String issuerHash = issuerHash(realm);
        OAuthSecurityTargetEnum targetType = OAuthSecurityTargetEnum.fromCode(changedEvent.targetType());
        if (targetType.isSecurityStateTarget()) {
            applySecurityState(realm, issuerHash, changedEvent, targetType);
        } else if (targetType.isRevocationTarget()) {
            applyRevocation(realm, issuerHash, changedEvent, targetType);
        } else {
            throw unavailable("OAUTH_SECURITY_TARGET_INVALID");
        }
    }

    /** Returns true only when this issuer's shared projection is ready and covers current durable state. */
    public boolean isIssuerReady(@NotNull @jakarta.validation.Valid TrustedRealmBO trustedRealm) {
        TrustedRealmBO realm = Objects.requireNonNull(trustedRealm, "trustedRealm");
        realm.validateTrustedPartition();
        String issuerHash = issuerHash(realm);
        String prefix = REDIS_PREFIX.formatted(issuerHash);
        try {
            RMap<String, String> ready = redisson.getMap(prefix + "ready", StringCodec.INSTANCE);
            Map<String, String> fields = ready.getAll(Set.of("epoch", "lastRevision", "status"));
            if (!revocationProperties.getEpoch().equals(fields.get("epoch"))
                    || !"READY".equals(fields.get("status"))) {
                return false;
            }
            long revision = Long.parseLong(Objects.requireNonNull(fields.get("lastRevision"), "lastRevision"));
            // The repository cursor is (revision,id); skip every row already covered by this revision.
            return stateRepository.findByRevisionAfter(realm, revision, Long.MAX_VALUE, 1).isEmpty()
                    && revocationRepository.findByProjectionStatus(realm, "PENDING", revision, Long.MAX_VALUE, 1).isEmpty()
                    && revocationRepository.findByProjectionStatus(realm, "APPLIED", revision, Long.MAX_VALUE, 1).isEmpty();
        } catch (DataAccessException | RedisException | NumberFormatException | NullPointerException failure) {
            log.warn("OAuth issuer projection is not ready; issuerHash={}", issuerHash);
            return false;
        }
    }

    public long recover(@NotNull @jakarta.validation.Valid TrustedRealmBO trustedRealm) {
        TrustedRealmBO realm = Objects.requireNonNull(trustedRealm, "trustedRealm");
        realm.validateTrustedPartition();
        String issuerHash = issuerHash(realm);
        String prefix = REDIS_PREFIX.formatted(issuerHash);
        RLock recoveryLock = redisson.getLock(RECOVERY_LOCK_PREFIX.formatted(issuerHash));
        if (!recoveryLock.tryLock()) {
            throw unavailable("OAUTH_SECURITY_RECOVERY_IN_PROGRESS");
        }
        try {
            beginRecovery(prefix + "ready");
            redisson.getKeys().deleteByPattern(prefix + "*");
            beginRecovery(prefix + "ready");
            initializeMissingRealmAndClientStates(realm);
            long latestRevision = rebuildSecurityStates(realm, prefix);
            latestRevision = Math.max(latestRevision, rebuildRevocations(realm, prefix, "PENDING"));
            latestRevision = Math.max(latestRevision, rebuildRevocations(realm, prefix, "APPLIED"));
            completeRecovery(prefix + "ready", latestRevision);
            return latestRevision;
        } catch (DataAccessException | RedisException failure) {
            log.error("OAuth security projection recovery failed; issuerHash={}", issuerHash, failure);
            throw unavailable("OAUTH_SECURITY_RECOVERY_UNAVAILABLE");
        } finally {
            recoveryLock.unlock();
        }
    }

    public void recoverRegisteredRealms() {
        TrustedRealmBO platformRealm = managementRealmResolver.platformRealm();
        for (OAuthRealmBO registeredRealm : realmRepository.findAllByStoragePartition(platformRealm)) {
            recover(trustedRealm(registeredRealm));
        }
    }

    public OAuthSecuritySnapshotBO snapshotForAuthorization(
            TrustedRealmBO trustedRealm,
            Long clientPk,
            String principalType,
            String identitySub) {
        TrustedRealmBO realm = Objects.requireNonNull(trustedRealm, "trustedRealm");
        realm.validateTrustedPartition();
        if (clientPk == null || clientPk <= 0L || !List.of("USER", "SERVICE").contains(principalType)) {
            throw unavailable("OAUTH_SECURITY_SNAPSHOT_INPUT_INVALID");
        }
        boolean user = "USER".equals(principalType);
        if (user != (identitySub != null && !identitySub.isBlank())) {
            throw unavailable("OAUTH_SECURITY_SNAPSHOT_SUBJECT_INVALID");
        }

        String issuerHash = issuerHash(realm);
        OAuthSecurityStateBO realmState;
        OAuthSecurityStateBO clientState;
        OAuthSecurityStateBO subjectState;
        List<Object> values;
        try {
            realmState = requireEnabled(realm, OAuthSecurityTargetEnum.REALM, "self");
            clientState = requireEnabled(realm, OAuthSecurityTargetEnum.CLIENT, Long.toString(clientPk));
            subjectState = user
                    ? requireSubjectState(realm, identitySub,
                            Math.max(realmState.getRevision(), clientState.getRevision()))
                    : null;
            String prefix = REDIS_PREFIX.formatted(issuerHash);
            List<Object> keys = new ArrayList<>(List.of(
                    prefix + "ready",
                    prefix + "generation:realm",
                    prefix + "generation:client:" + clientPk));
            if (user) {
                keys.add(prefix + "generation:subject:" + Digests.sha256Hex(identitySub));
            }
            values = redisson.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_ONLY, SNAPSHOT_SCRIPT, RScript.ReturnType.MULTI, keys);
        } catch (DataAccessException | RedisException failure) {
            log.warn("OAuth security snapshot dependency is unavailable; issuerHash={}", issuerHash);
            throw unavailable("OAUTH_SECURITY_SNAPSHOT_UNAVAILABLE");
        }
        if (values == null || values.size() != (user ? 12 : 9)) {
            throw unavailable("OAUTH_SECURITY_SNAPSHOT_UNAVAILABLE");
        }

        String epoch = value(values, 0);
        long readyRevision = number(values, 1);
        if (epoch.isBlank() || !revocationProperties.getEpoch().equals(epoch)
                || !"READY".equals(value(values, 2))) {
            throw unavailable("OAUTH_SECURITY_READY_NOT_AVAILABLE");
        }
        verifyRealmProjection(values, 3, realmState);
        verifyProjection(values, 6, clientState);
        if (user) {
            verifyProjection(values, 9, subjectState);
        }
        long latestRevision = Math.max(realmState.getRevision(), clientState.getRevision());
        if (subjectState != null) {
            latestRevision = Math.max(latestRevision, subjectState.getRevision());
        }
        if (readyRevision < latestRevision) {
            throw unavailable("OAUTH_SECURITY_READY_REVISION_STALE");
        }

        return OAuthSecuritySnapshotBO.builder()
                .realmGeneration(realmState.getGeneration())
                .clientGeneration(clientState.getGeneration())
                .subjectGeneration(subjectState == null ? null : subjectState.getGeneration())
                .readyEpoch(epoch)
                .revision(latestRevision)
                .build();
    }

    public void requireCurrentSnapshot(
            TrustedRealmBO trustedRealm,
            Long clientPk,
            String principalType,
            String identitySub,
            OAuthSecuritySnapshotBO expected) {
        OAuthSecuritySnapshotBO current = snapshotForAuthorization(
                trustedRealm, clientPk, principalType, identitySub);
        if (expected == null
                || current.getRealmGeneration() != expected.getRealmGeneration()
                || current.getClientGeneration() != expected.getClientGeneration()
                || !Objects.equals(current.getSubjectGeneration(), expected.getSubjectGeneration())) {
            throw unavailable("OAUTH_SECURITY_SNAPSHOT_STALE");
        }
    }

    private void applySecurityState(
            TrustedRealmBO realm,
            String issuerHash,
            OAuthSecurityChangedEvent event,
            OAuthSecurityTargetEnum targetType) {
        OAuthSecurityStateBO state = stateRepository.findByTarget(realm, targetType, event.targetKey())
                .orElseThrow(() -> unavailable("OAUTH_SECURITY_STATE_MISSING"));
        if (event.revision() > state.getRevision()) {
            throw unavailable("OAUTH_SECURITY_EVENT_REVISION_AHEAD");
        }
        String prefix = REDIS_PREFIX.formatted(issuerHash);
        applyProjection(prefix + "ready", generationKey(prefix, targetType, event.targetKey()),
                "GENERATION", targetType, state.getRevision(), state.getGeneration(),
                state.isEnabled(), null, state.getReasonCode(), false);
    }

    private void applyRevocation(
            TrustedRealmBO realm,
            String issuerHash,
            OAuthSecurityChangedEvent event,
            OAuthSecurityTargetEnum targetType) {
        Optional<OAuthRevocationBO> found = revocationRepository.findByTarget(realm, targetType, event.targetKey());
        if (found.isEmpty()) {
            if (event.expiresAt() != null && !event.expiresAt().isAfter(clock.instant())) {
                String prefix = REDIS_PREFIX.formatted(issuerHash);
                applyProjection(prefix + "ready", denyKey(issuerHash, targetType, event.targetKey()),
                        "DENY", targetType,
                        event.revision(), 0L, false, event.expiresAt(), event.reasonCode(), true);
                return;
            }
            throw unavailable("OAUTH_REVOCATION_STATE_MISSING");
        }
        OAuthRevocationBO revocation = found.get();
        if (event.revision() > revocation.getRevision()) {
            throw unavailable("OAUTH_SECURITY_EVENT_REVISION_AHEAD");
        }
        String prefix = REDIS_PREFIX.formatted(issuerHash);
        applyProjection(prefix + "ready", denyKey(issuerHash, targetType, revocation.getTargetKey()), "DENY",
                targetType, revocation.getRevision(), 0L, false,
                revocation.getExpiresAt(), revocation.getReasonCode(), false);
    }

    private long rebuildSecurityStates(TrustedRealmBO realm, String prefix) {
        long afterRevision = 0L;
        long afterId = 0L;
        long latestRevision = 0L;
        while (true) {
            List<OAuthSecurityStateBO> page = stateRepository.findByRevisionAfter(
                    realm, afterRevision, afterId, RECOVERY_PAGE_SIZE);
            if (page.isEmpty()) {
                return latestRevision;
            }
            for (OAuthSecurityStateBO state : page) {
                if (!state.getTargetType().isSecurityStateTarget()) {
                    throw unavailable("OAUTH_SECURITY_STATE_TARGET_INVALID");
                }
                writeRecoveryProjection(generationKey(prefix, state.getTargetType(), state.getTargetKey()),
                        "GENERATION", state.getTargetType(), state.getRevision(), state.getGeneration(),
                        state.isEnabled(), null, state.getReasonCode());
                latestRevision = Math.max(latestRevision, state.getRevision());
                afterRevision = state.getRevision();
                afterId = Objects.requireNonNull(state.getId(), "securityStateId");
            }
            if (page.size() < RECOVERY_PAGE_SIZE) {
                return latestRevision;
            }
        }
    }

    private long rebuildRevocations(TrustedRealmBO realm, String prefix, String status) {
        long afterRevision = 0L;
        long afterId = 0L;
        long latestRevision = 0L;
        Instant now = clock.instant();
        while (true) {
            List<OAuthRevocationBO> page = revocationRepository.findByProjectionStatus(
                    realm, status, afterRevision, afterId, RECOVERY_PAGE_SIZE);
            if (page.isEmpty()) {
                return latestRevision;
            }
            for (OAuthRevocationBO revocation : page) {
                OAuthSecurityTargetEnum targetType = revocation.getTargetType();
                if (targetType == null || !targetType.isRevocationTarget()) {
                    throw unavailable("OAUTH_REVOCATION_TARGET_INVALID");
                }
                latestRevision = Math.max(latestRevision, revocation.getRevision());
                if (targetType == OAuthSecurityTargetEnum.KID
                        || revocation.getExpiresAt() != null && revocation.getExpiresAt().isAfter(now)) {
                    writeRecoveryProjection(denyKeyFromPrefix(prefix, targetType, revocation.getTargetKey()), "DENY",
                            targetType, revocation.getRevision(), 0L, false,
                            revocation.getExpiresAt(), revocation.getReasonCode());
                    if ("PENDING".equals(status)) {
                        revocationRepository.save(realm, applied(revocation), revocation.getVersion());
                    }
                }
                afterRevision = revocation.getRevision();
                afterId = Objects.requireNonNull(revocation.getId(), "revocationId");
            }
            if (page.size() < RECOVERY_PAGE_SIZE) {
                return latestRevision;
            }
        }
    }

    private void applyProjection(
            String readyKey,
            String targetKey,
            String projectionKind,
            OAuthSecurityTargetEnum targetType,
            long revision,
            long generation,
            boolean enabled,
            Instant expiresAt,
            String reasonCode,
            boolean allowStale) {
        Number result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                APPLY_PROJECTION_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(readyKey, targetKey),
                revocationProperties.getEpoch(),
                projectionKind,
                Long.toString(revision),
                projectionKind.equals("GENERATION") ? Long.toString(generation) : "",
                projectionKind.equals("GENERATION") ? (enabled ? "1" : "0") : "",
                expiresAt == null ? "0" : Long.toString(expiresAt.toEpochMilli()),
                reasonCode,
                targetType.getCode(),
                Long.toString(clock.millis()));
        if (allowStale && result != null && result.longValue() == 0L) {
            return;
        }
        requireApplied(result, "OAUTH_SECURITY_PROJECTION_NOT_CONFIRMED");
    }

    private void writeRecoveryProjection(
            String targetKey,
            String projectionKind,
            OAuthSecurityTargetEnum targetType,
            long revision,
            long generation,
            boolean enabled,
            Instant expiresAt,
            String reasonCode) {
        Number result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                RECOVERY_WRITE_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(targetKey),
                projectionKind,
                targetType.getCode(),
                Long.toString(revision),
                projectionKind.equals("GENERATION") ? Long.toString(generation) : "",
                projectionKind.equals("GENERATION") ? (enabled ? "1" : "0") : "",
                expiresAt == null ? "0" : Long.toString(expiresAt.toEpochMilli()),
                reasonCode);
        if (result == null || result.longValue() < 1L) {
            throw unavailable("OAUTH_SECURITY_RECOVERY_PROJECTION_NOT_CONFIRMED");
        }
    }

    private void beginRecovery(String readyKey) {
        Number result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                BEGIN_RECOVERY_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(readyKey),
                revocationProperties.getEpoch());
        if (result == null || result.longValue() != 1L) {
            throw unavailable("OAUTH_SECURITY_RECOVERY_FENCE_FAILED");
        }
    }

    private void completeRecovery(String readyKey, long latestRevision) {
        Number result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                COMPLETE_RECOVERY_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(readyKey),
                revocationProperties.getEpoch(),
                Long.toString(latestRevision));
        if (result == null || result.longValue() != 1L) {
            throw unavailable("OAUTH_SECURITY_RECOVERY_READY_FAILED");
        }
    }

    private OAuthRevocationBO applied(OAuthRevocationBO current) {
        return OAuthRevocationBO.builder()
                .id(current.getId())
                .version(Math.addExact(current.getVersion(), 1L))
                .targetType(current.getTargetType())
                .targetKey(current.getTargetKey())
                .reasonCode(current.getReasonCode())
                .revokedAt(current.getRevokedAt())
                .expiresAt(current.getExpiresAt())
                .revision(current.getRevision())
                .projectionStatus("APPLIED")
                .build();
    }

    private OAuthSecurityStateBO requireEnabled(
            TrustedRealmBO trustedRealm,
            OAuthSecurityTargetEnum target,
            String targetKey) {
        OAuthSecurityStateBO state = stateRepository.findByTarget(trustedRealm, target, targetKey)
                .orElseThrow(() -> unavailable("OAUTH_SECURITY_STATE_MISSING"));
        if (!state.isEnabled()) {
            throw invalidGrant("OAUTH_SECURITY_STATE_DISABLED");
        }
        return state;
    }

    private OAuthSecurityStateBO requireSubjectState(TrustedRealmBO realm, String subject, long revisionFloor) {
        Optional<OAuthSecurityStateBO> existing = stateRepository.findByTarget(
                realm, OAuthSecurityTargetEnum.SUBJECT, subject);
        if (existing.isPresent()) {
            if (!existing.get().isEnabled()) {
                throw invalidGrant("OAUTH_SECURITY_STATE_DISABLED");
            }
            return existing.get();
        }
        if (authorizationRepository.findByPrincipalName(realm, subject, "ACTIVE", 0L, 1).size() > 0
                || authorizationRepository.findByPrincipalName(realm, subject, "REVOKED", 0L, 1).size() > 0
                || authorizationRepository.findByPrincipalName(realm, subject, "EXPIRED", 0L, 1).size() > 0) {
            throw unavailable("OAUTH_SECURITY_SUBJECT_STATE_MISSING_WITH_AUTHORIZATION");
        }
        OAuthSecurityStateBO initial = OAuthSecurityStateBO.builder()
                .version(0L)
                .targetType(OAuthSecurityTargetEnum.SUBJECT)
                .targetKey(subject)
                .generation(0L)
                .enabled(true)
                .revision(revisionFloor)
                .reasonCode("INITIALIZED")
                .build();
        OAuthSecurityStateBO saved = stateRepository.save(realm, initial, 0L);
        OAuthSecurityChangedEvent initialized = new OAuthSecurityChangedEvent(
                Digests.sha256Hex(realm.getRealmKey() + ":SUBJECT:" + subject + ":INITIALIZED"),
                1,
                realm.getIssuer(),
                realm.getRealmKey(),
                saved.getRevision(),
                OAuthSecurityTargetEnum.SUBJECT.getCode(),
                subject,
                saved.getGeneration(),
                null,
                saved.getReasonCode(),
                clock.instant(),
                null);
        applyProjection(realm, initialized);
        return saved;
    }

    private void initializeMissingRealmAndClientStates(TrustedRealmBO realm) {
        OAuthRealmBO registeredRealm = realmRepository.findByRealmKey(realm)
                .orElseThrow(() -> unavailable("OAUTH_SECURITY_REALM_STATE_MISSING"));
        List<OAuthClientBO> clients = clientRepository.findAllByRealm(realm);
        boolean realmMissing = stateRepository.findByTarget(realm, OAuthSecurityTargetEnum.REALM, "self").isEmpty();
        List<OAuthClientBO> missingClients = clients.stream()
                .filter(client -> stateRepository.findByTarget(realm, OAuthSecurityTargetEnum.CLIENT,
                        Long.toString(Objects.requireNonNull(client.getId(), "clientId"))).isEmpty())
                .toList();
        if (!realmMissing && missingClients.isEmpty()) {
            return;
        }
        if (hasSecurityHistory(realm)) {
            throw unavailable("OAUTH_SECURITY_GENERATION_STATE_INCOMPLETE");
        }
        if (realmMissing) {
            stateRepository.save(realm, initialState(OAuthSecurityTargetEnum.REALM, "self",
                    "ACTIVE".equals(registeredRealm.getStatus())), 0L);
        }
        for (OAuthClientBO client : missingClients) {
            stateRepository.save(realm, initialState(OAuthSecurityTargetEnum.CLIENT,
                    Long.toString(client.getId()), client.getStatus() == OAuthClient.Status.ACTIVE), 0L);
        }
    }

    private boolean hasSecurityHistory(TrustedRealmBO realm) {
        return !authorizationRepository.findExpiredBefore(realm, MAX_AUTHORIZATION_CUTOFF, 0L, 1).isEmpty()
                || !tokenRepository.findExpiredBefore(realm, MAX_AUTHORIZATION_CUTOFF, 0L, 1).isEmpty()
                || !revocationRepository.findByProjectionStatus(realm, "PENDING", 0L, 0L, 1).isEmpty()
                || !revocationRepository.findByProjectionStatus(realm, "APPLIED", 0L, 0L, 1).isEmpty();
    }

    private static OAuthSecurityStateBO initialState(
            OAuthSecurityTargetEnum targetType,
            String targetKey,
            boolean enabled) {
        return OAuthSecurityStateBO.builder()
                .version(0L)
                .targetType(targetType)
                .targetKey(targetKey)
                .generation(0L)
                .enabled(enabled)
                .revision(0L)
                .reasonCode("INITIALIZED")
                .build();
    }

    private static TrustedRealmBO trustedRealm(OAuthRealmBO realm) {
        OAuthRealmBO registered = Objects.requireNonNull(realm, "registeredRealm");
        TrustedRealmBO trusted = TrustedRealmBO.builder()
                .issuer(registered.getIssuer())
                .realmKey(registered.getRealmKey())
                .realmType(registered.getRealmType())
                .storagePartitionId(registered.getStoragePartitionId())
                .businessTenantId(registered.getBusinessTenantId())
                .trustVersion(registered.getTrustVersion())
                .build();
        trusted.validateTrustedPartition();
        return trusted;
    }

    private static String generationKey(String prefix, OAuthSecurityTargetEnum targetType, String targetKey) {
        return switch (targetType) {
            case REALM -> prefix + "generation:realm";
            case CLIENT -> prefix + "generation:client:" + targetKey;
            case SUBJECT -> prefix + "generation:subject:" + Digests.sha256Hex(targetKey);
            default -> throw unavailable("OAUTH_SECURITY_STATE_TARGET_INVALID");
        };
    }

    private static String denyKey(String issuerHash, OAuthSecurityTargetEnum targetType, String targetKey) {
        return denyKey(REDIS_PREFIX.formatted(issuerHash), targetType, targetKey);
    }

    private static String denyKeyFromPrefix(String prefix, OAuthSecurityTargetEnum targetType, String targetKey) {
        return switch (targetType) {
            case JTI -> prefix + "deny:jti:" + targetKey;
            case AUTHORIZATION -> prefix + "deny:authorization:" + targetKey;
            case KID -> prefix + "deny:kid:" + targetKey;
            default -> throw unavailable("OAUTH_REVOCATION_TARGET_INVALID");
        };
    }

    private static void requireApplied(Number result, String failureCode) {
        if (result == null) {
            throw unavailable(failureCode);
        }
        long status = result.longValue();
        if (status < 1L) {
            throw unavailable(switch ((int) status) {
                case -1 -> "OAUTH_SECURITY_READY_NOT_AVAILABLE";
                case -2 -> "OAUTH_SECURITY_PROJECTION_REVISION_CONFLICT";
                case 0 -> "OAUTH_SECURITY_PROJECTION_STALE";
                default -> failureCode;
            });
        }
    }

    private static void requireMatchingRealm(TrustedRealmBO realm, OAuthSecurityChangedEvent event) {
        if (!Objects.equals(realm.getIssuer().normalize(), event.issuer().normalize())
                || !Objects.equals(realm.getRealmKey(), event.realmKey())) {
            throw unavailable("OAUTH_SECURITY_EVENT_REALM_MISMATCH");
        }
    }

    private static String issuerHash(TrustedRealmBO realm) {
        return Digests.sha256Hex(realm.getIssuer().normalize().toASCIIString());
    }

    private static void verifyProjection(List<Object> values, int offset, OAuthSecurityStateBO state) {
        if (!Long.toString(state.getGeneration()).equals(value(values, offset))
                || !isEnabled(value(values, offset + 1))
                || !Long.toString(state.getRevision()).equals(value(values, offset + 2))) {
            throw unavailable("OAUTH_SECURITY_PROJECTION_STALE");
        }
    }

    /**
     * Realm revision also allocates revisions for client and subject changes. Its projection may therefore
     * carry an earlier revision with the same generation; the READY watermark below still covers every DB revision.
     * Realm generation/enabled checks and exact client/subject projection checks remain mandatory.
     */
    private static void verifyRealmProjection(List<Object> values, int offset, OAuthSecurityStateBO state) {
        if (!Long.toString(state.getGeneration()).equals(value(values, offset))
                || !isEnabled(value(values, offset + 1))
                || number(values, offset + 2) > state.getRevision()) {
            throw unavailable("OAUTH_SECURITY_PROJECTION_STALE");
        }
    }

    private static boolean isEnabled(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private static String value(List<Object> values, int index) {
        Object value = values.get(index);
        return value == null ? "" : value.toString();
    }

    private static long number(List<Object> values, int index) {
        try {
            return Long.parseLong(value(values, index));
        } catch (NumberFormatException failure) {
            throw unavailable("OAUTH_SECURITY_REVISION_INVALID");
        }
    }

    private static OAuth2AuthenticationException unavailable(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error("temporarily_unavailable", code, null));
    }

    private static OAuth2AuthenticationException invalidGrant(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", code, null));
    }
}
