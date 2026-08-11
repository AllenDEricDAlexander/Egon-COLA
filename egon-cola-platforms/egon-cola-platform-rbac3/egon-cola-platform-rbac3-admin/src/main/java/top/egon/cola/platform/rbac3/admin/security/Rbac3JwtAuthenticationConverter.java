package top.egon.cola.platform.rbac3.admin.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.snapshot.infrastructure.RedisAuthorizationRuntimeStore;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 将已验证的用户 Bearer Claim 转换为 RBAC3 用户主体。
 * Converts validated user bearer claims into the RBAC3 user principal.
 */
public final class Rbac3JwtAuthenticationConverter
        implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final RedisAuthorizationRuntimeStore runtimeStore;

    public Rbac3JwtAuthenticationConverter(
            RedisAuthorizationRuntimeStore runtimeStore) {
        this.runtimeStore = runtimeStore;
    }

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        CurrentRbac3Principal principal = user(jwt);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.authorities());
    }

    private CurrentRbac3Principal user(Jwt jwt) {
        String tenantId = required(jwt.getClaimAsString("tid"), "tid");
        String identitySub = required(jwt.getSubject(), "sub");
        String sessionId = required(jwt.getClaimAsString("sid"), "sid");
        long authVersion = number(jwt, "av");
        long sessionVersion = number(jwt, "sv");
        long policyVersion = number(jwt, "pv");
        AuthorizationDecisionService.SnapshotRecord record = runtimeStore.load(
                tenantId, sessionId);
        if (!record.tenantId().equals(tenantId)
                || !record.identitySub().equals(identitySub)
                || record.snapshot().authVersion() != authVersion
                || record.snapshot().sessionVersion() != sessionVersion
                || record.snapshot().policyVersion() != policyVersion) {
            throw new Rbac3RuleViolation("AUTHORIZATION_VERSION_MISMATCH");
        }
        Set<String> permissions = new LinkedHashSet<>();
        record.snapshot().appContexts().forEach(
                context -> permissions.addAll(context.permissions()));
        return new CurrentRbac3Principal(
                tenantId, identitySub, record.userId(), sessionId,
                authVersion, sessionVersion,
                policyVersion, permissions,
                Boolean.TRUE.equals(jwt.getClaim("platform_administrator")));
    }

    private long number(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        if (!(value instanceof Number number) || number.longValue() < 0L) {
            throw new IllegalArgumentException(name + " must be a non-negative number");
        }
        return number.longValue();
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
