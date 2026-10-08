package top.egon.cola.platform.tianquan.shoubing.starter.security;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpClaimNames;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;

/** Maps a signature-verified JWT to the existing identity-only security principals. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VerifiedPrincipalConverter
        extends BaseForwardConverter<Jwt, IdentityPrincipal> {

    @Override
    @Mapping(target = "subject", expression = "java(required(source.getSubject(), \"sub\"))")
    @Mapping(target = "tenantId", expression = "java(required(source.getClaimAsString(\"tenant_id\"), \"tenant_id\"))")
    @Mapping(target = "tokenId", expression = "java(required(source.getClaimAsString(\"jti\"), \"jti\"))")
    @Mapping(target = "audience", expression = "java(audience(source))")
    @Mapping(target = "issuedAt", expression = "java(required(source.getIssuedAt(), \"iat\"))")
    @Mapping(target = "expiresAt", expression = "java(required(source.getExpiresAt(), \"exp\"))")
    @Mapping(target = "authenticationContext", expression = "java(authenticationContext(source))")
    IdentityPrincipal toTarget(Jwt source);

    @Mapping(target = "subject", expression = "java(required(source.getSubject(), \"sub\"))")
    @Mapping(target = "tenantId", expression = "java(source.getClaimAsString(\"tenant_id\"))")
    @Mapping(target = "clientId", expression = "java(required(source.getClaimAsString(\"client_id\"), \"client_id\"))")
    @Mapping(target = "tokenId", expression = "java(required(source.getClaimAsString(\"jti\"), \"jti\"))")
    @Mapping(target = "resourceUri", expression = "java(required(resourceUri, \"resourceUri\"))")
    @Mapping(target = "resourceVersion", expression = "java(nonNegativeLong(source, \"resource_version\"))")
    @Mapping(target = "scopes", expression = "java(scopes(source))")
    @Mapping(target = "sourceBizCode", expression = "java(required(source.getClaimAsString(\"source_biz\"), \"source_biz\"))")
    @Mapping(target = "sourceAppCode", expression = "java(required(source.getClaimAsString(\"source_app\"), \"source_app\"))")
    @Mapping(target = "sourceEnvironment", expression = "java(required(source.getClaimAsString(\"source_env\"), \"source_env\"))")
    @Mapping(target = "credentialId", expression = "java(required(source.getClaimAsString(\"credential_id\"), \"credential_id\"))")
    @Mapping(target = "issuedAt", expression = "java(required(source.getIssuedAt(), \"iat\"))")
    @Mapping(target = "expiresAt", expression = "java(required(source.getExpiresAt(), \"exp\"))")
    @Mapping(target = "appId", expression = "java(required(source.getClaimAsString(\"app_id\"), \"app_id\"))")
    @Mapping(target = "scopeContext", expression = "java(scopeContext(source))")
    ServiceIdentityPrincipal toServicePrincipal(Jwt source, @Context URI resourceUri);

    default AuthenticationContext authenticationContext(Jwt source) {
        Instant issuedAt = required(source.getIssuedAt(), "iat");
        String acr = source.getClaimAsString("acr");
        if (acr == null || acr.isBlank()) {
            acr = "PASSWORD";
        }
        Object authTimeClaim = source.getClaims().get("auth_time");
        Instant authTime;
        if (authTimeClaim == null) {
            authTime = issuedAt;
        } else if (authTimeClaim instanceof Instant instant) {
            authTime = instant;
        } else if (authTimeClaim instanceof Number seconds && seconds.longValue() >= 0L) {
            authTime = Instant.ofEpochSecond(seconds.longValue());
        } else {
            throw new IllegalArgumentException("JWT_CLAIM_INVALID_AUTH_TIME");
        }
        return AuthenticationContext.of(acr, authTime);
    }

    default Set<String> audience(Jwt source) {
        List<String> values = source.getAudience();
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("JWT_AUDIENCE_INVALID");
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            result.add(required(value, "aud"));
        }
        if (result.size() != values.size()) {
            throw new IllegalArgumentException("JWT_AUDIENCE_INVALID");
        }
        return Set.copyOf(result);
    }

    default Set<String> scopes(Jwt source) {
        Object raw = source.getClaims().get(IdpClaimNames.SCOPE);
        Collection<?> values;
        if (raw instanceof String text && !text.isBlank()) {
            values = List.of(text.trim().split("\\s+"));
        } else if (raw instanceof Collection<?> collection) {
            values = collection;
        } else {
            throw new IllegalArgumentException("JWT_SCOPE_INVALID");
        }
        TreeSet<String> result = new TreeSet<>();
        for (Object value : values) {
            String scope = required(value instanceof String text ? text : null, IdpClaimNames.SCOPE);
            if (!scope.matches("[A-Za-z0-9:._-]{1,128}") || !result.add(scope)) {
                throw new IllegalArgumentException("JWT_SCOPE_INVALID");
            }
        }
        if (result.isEmpty() || result.size() > 64) {
            throw new IllegalArgumentException("JWT_SCOPE_INVALID");
        }
        return Set.copyOf(result);
    }

    default long nonNegativeLong(Jwt source, String claim) {
        Object raw = source.getClaims().get(claim);
        if (!(raw instanceof Number number) || number.longValue() < 0L) {
            throw new IllegalArgumentException("JWT_CLAIM_INVALID_" + claim.toUpperCase());
        }
        return number.longValue();
    }

    default ServiceTokenContext scopeContext(Jwt source) {
        String value = required(source.getClaimAsString(IdpClaimNames.SCOPE_CONTEXT),
                IdpClaimNames.SCOPE_CONTEXT);
        try {
            return ServiceTokenContext.valueOf(value);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("SERVICE_CONTEXT_INVALID", invalid);
        }
    }

    default String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("JWT_CLAIM_INVALID_" + field.toUpperCase());
        }
        return value;
    }

    default <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("JWT_CLAIM_INVALID_" + field.toUpperCase());
        }
        return value;
    }
}
