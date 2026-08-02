package top.egon.cola.platform.rbac3.admin.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.snapshot.infrastructure.RedisAuthorizationRuntimeStore;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts validated bearer claims into the principal types consumed by RBAC3 APIs.
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
        Object principal = isService(jwt) ? service(jwt) : user(jwt);
        var authorities = principal instanceof CurrentRbac3Principal user
                ? user.authorities()
                : ((CurrentRbac3ServicePrincipal) principal).authorities();
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, authorities);
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

    private CurrentRbac3ServicePrincipal service(Jwt jwt) {
        return new CurrentRbac3ServicePrincipal(
                required(jwt.getClaimAsString("tid"), "tid"),
                required(first(jwt, "service_id", "client_id"), "service_id"),
                required(first(jwt, "application_code", "app"), "application_code"),
                required(jwt.getClaimAsString("env"), "env"),
                required(jwt.getClaimAsString("namespace"), "namespace"),
                required(first(jwt, "credential_id", "jti"), "credential_id"),
                permissions(jwt));
    }

    private boolean isService(Jwt jwt) {
        return "SERVICE".equalsIgnoreCase(jwt.getClaimAsString("principal_type"))
                || jwt.hasClaim("service_id")
                || jwt.hasClaim("client_id");
    }

    private Set<String> permissions(Jwt jwt) {
        Set<String> values = new LinkedHashSet<>();
        Object permissions = jwt.getClaim("permissions");
        if (permissions instanceof Collection<?> collection) {
            collection.stream().map(String::valueOf)
                    .filter(value -> !value.isBlank()).forEach(values::add);
        } else if (permissions instanceof String text) {
            split(text).forEach(values::add);
        }
        String scope = jwt.getClaimAsString("scope");
        if (scope != null) {
            split(scope).forEach(values::add);
        }
        return values;
    }

    private List<String> split(String value) {
        return java.util.Arrays.stream(value.trim().split("[\\s,]+"))
                .filter(item -> !item.isBlank()).toList();
    }

    private String first(Jwt jwt, String first, String second) {
        String value = jwt.getClaimAsString(first);
        return value == null || value.isBlank()
                ? jwt.getClaimAsString(second) : value;
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
