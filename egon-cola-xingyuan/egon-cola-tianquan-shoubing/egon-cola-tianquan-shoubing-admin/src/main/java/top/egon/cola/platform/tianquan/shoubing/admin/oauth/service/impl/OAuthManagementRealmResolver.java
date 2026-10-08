package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

@Component("oauthManagementRealmResolver")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthManagementRealmResolver {

    private static final Pattern TENANT_REALM = Pattern.compile("tenant:([1-9][0-9]{0,18})");
    private static final Duration PLATFORM_AUTHENTICATION_MAX_AGE = Duration.ofMinutes(5);

    @NonNull
    @Qualifier("oauthPublicOrigin")
    private final URI publicOrigin;
    @NonNull
    @Qualifier("tenantIssuerService")
    private final TenantIssuerService tenantIssuerService;
    @Value("${egon.tianquan-shoubing.oauth.platform-management-tenant-id}")
    private final long platformManagementTenantId;
    @NonNull
    @Qualifier("idpClock")
    private final Clock clock;

    @PostConstruct
    void validateConfiguration() {
        requireOrigin(publicOrigin);
        if (platformManagementTenantId <= 0L) {
            throw new IllegalArgumentException("OAUTH_PLATFORM_MANAGEMENT_TENANT_INVALID");
        }
    }

    public TrustedRealmBO resolve(IdentityPrincipal principal, String requestedRealm) {
        IdentityPrincipal identity = Objects.requireNonNull(principal, "principal");
        long principalTenantId = tenantId(identity.tenantId());
        String realm = requestedRealm == null ? "tenant:" + principalTenantId : requestedRealm;
        if (realm == null || realm.isBlank() || !realm.equals(realm.trim())) {
            throw new IllegalArgumentException("OAUTH_MANAGEMENT_REALM_INVALID");
        }
        if ("platform".equals(realm)) {
            requirePlatformAdministrator(identity, principalTenantId);
            return platformRealm();
        }

        Matcher matcher = TENANT_REALM.matcher(realm);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("OAUTH_MANAGEMENT_REALM_INVALID");
        }
        long requestedTenantId = parseTenantId(matcher.group(1));
        if (requestedTenantId != principalTenantId) {
            throw new AccessDeniedException("OAUTH_MANAGEMENT_REALM_FORBIDDEN");
        }
        return tenantIssuerService.resolveExactIssuer(issuer("/tenants/" + requestedTenantId));
    }

    public TrustedRealmBO platformRealm() {
        return tenantIssuerService.resolveExactIssuer(issuer("/platform"));
    }

    public TrustedRealmBO resolveTenantRealm(String tenantId) {
        long id = parseTenantId(requiredTenantId(tenantId));
        return tenantIssuerService.resolveExactIssuer(issuer("/tenants/" + id));
    }

    private void requirePlatformAdministrator(IdentityPrincipal principal, long principalTenantId) {
        AuthenticationContext authentication = principal.authenticationContext();
        if (principalTenantId != platformManagementTenantId
                || authentication == null
                || authentication.authTime().isAfter(clock.instant())
                || !authentication.satisfies("STRONG", PLATFORM_AUTHENTICATION_MAX_AGE, clock.instant())) {
            throw new AccessDeniedException("OAUTH_PLATFORM_MANAGEMENT_FORBIDDEN");
        }
    }

    private URI issuer(String path) {
        String origin = publicOrigin.toASCIIString();
        return URI.create((origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin) + path);
    }

    private static URI requireOrigin(URI origin) {
        URI value = Objects.requireNonNull(origin, "publicOrigin");
        if (!value.isAbsolute() || !"https".equalsIgnoreCase(value.getScheme()) || value.getHost() == null
                || value.getUserInfo() != null || value.getQuery() != null || value.getFragment() != null
                || (value.getRawPath() != null && !value.getRawPath().isEmpty() && !"/".equals(value.getRawPath()))) {
            throw new IllegalArgumentException("OAUTH_PUBLIC_ORIGIN_INVALID");
        }
        return value;
    }

    private static long tenantId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("OAUTH_MANAGEMENT_TENANT_INVALID");
        }
        return parseTenantId(value);
    }

    private static long parseTenantId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0L) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("OAUTH_MANAGEMENT_TENANT_INVALID", failure);
        }
    }

    private static String requiredTenantId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("OAUTH_MANAGEMENT_TENANT_INVALID");
        }
        return value;
    }
}
