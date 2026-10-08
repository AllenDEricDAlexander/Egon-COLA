package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSecurityStateRepository;

@Component("oauthSecurityProjectionComponent")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthSecurityProjectionComponent {

    private static final String REDIS_PREFIX = "egon:oauth:security:v2:{%s}:";
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

    @Qualifier("rbac3RuntimeRedissonClient")
    private final RedissonClient redisson;
    @Qualifier("oAuthSecurityStateRepository")
    private final OAuthSecurityStateRepository stateRepository;
    @Value("${egon.cola.platform.tianquan.shoubing.revocation.epoch}")
    private final String configuredEpoch;

    @PostConstruct
    void validateConfiguredEpoch() {
        if (configuredEpoch == null || !configuredEpoch.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) {
            throw new IllegalStateException("OAUTH_REVOCATION_EPOCH_INVALID");
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

        String issuerHash = Digests.sha256Hex(realm.getIssuer().normalize().toASCIIString());
        OAuthSecurityStateBO realmState;
        OAuthSecurityStateBO clientState;
        OAuthSecurityStateBO subjectState;
        List<Object> values;
        try {
            realmState = requireEnabled(realm, OAuthSecurityTargetEnum.REALM, "self");
            clientState = requireEnabled(realm, OAuthSecurityTargetEnum.CLIENT, Long.toString(clientPk));
            subjectState = user
                    ? requireEnabled(realm, OAuthSecurityTargetEnum.SUBJECT, identitySub)
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
        if (epoch.isBlank() || !configuredEpoch.equals(epoch) || !"READY".equals(value(values, 2))) {
            throw unavailable("OAUTH_SECURITY_READY_NOT_AVAILABLE");
        }
        verifyProjection(values, 3, realmState);
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

    private OAuthSecurityStateBO requireEnabled(
            TrustedRealmBO trustedRealm,
            OAuthSecurityTargetEnum target,
            String targetKey) {
        OAuthSecurityStateBO state = stateRepository.findByTargetForUpdate(trustedRealm, target, targetKey)
                .orElseThrow(() -> unavailable("OAUTH_SECURITY_STATE_MISSING"));
        if (!state.isEnabled()) {
            throw invalidGrant("OAUTH_SECURITY_STATE_DISABLED");
        }
        return state;
    }

    private static void verifyProjection(List<Object> values, int offset, OAuthSecurityStateBO state) {
        if (!Long.toString(state.getGeneration()).equals(value(values, offset))
                || !isEnabled(value(values, offset + 1))
                || !Long.toString(state.getRevision()).equals(value(values, offset + 2))) {
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
        return new OAuth2AuthenticationException(
                new OAuth2Error("temporarily_unavailable", code, null));
    }

    private static OAuth2AuthenticationException invalidGrant(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_grant", code, null));
    }
}
