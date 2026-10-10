package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import jakarta.validation.Validator;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationConsentAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.TenantLoginAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command.ConsumeOAuthTokenCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthTokenIssueService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditContextBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter;

@RequiredArgsConstructor
@Slf4j
public final class TransactionalOAuthAuthenticationProvider implements AuthenticationProvider {

    @NonNull
    private final AuthenticationProvider delegate;
    @NonNull
    private final TransactionTemplate transactionTemplate;
    @NonNull
    @Qualifier("oauthTokenIssueService")
    private final OAuthTokenIssueService tokenIssueService;
    @NonNull
    @Qualifier("tenantIssuerService")
    private final TenantIssuerService tenantIssuerService;
    @NonNull
    private final GrantTransaction grantTransaction;

    @Override
    public Authentication authenticate(Authentication authentication) {
        AtomicReference<OAuth2AuthenticationException> committedDenial = new AtomicReference<>();
        try {
            Authentication result = transactionTemplate.execute(status -> {
                var trustedRealm = tenantIssuerService.resolveCurrentIssuer();
                try {
                    return grantTransaction.authenticate(authentication, delegate, trustedRealm, tokenIssueService);
                } catch (OAuth2AuthorizationCodeRequestAuthenticationException denied) {
                    if (!grantTransaction.commitsConsentDenial()
                            || !"access_denied".equals(denied.getError().getErrorCode())) {
                        throw denied;
                    }
                    committedDenial.set(denied);
                    return null;
                }
            });
            if (committedDenial.get() != null) {
                log.debug("Native authorization consent cancelled");
                throw committedDenial.get();
            }
            return result;
        } catch (DataAccessException | RedisException | JwtEncodingException failure) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("temporarily_unavailable", "Request temporarily unavailable", null), failure);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }

    public static GrantTransaction authorizationRequest(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return (authentication, delegate, realm, service) -> {
            OAuth2AuthorizationCodeRequestAuthenticationToken request =
                    (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
            Authentication principal = (Authentication) request.getPrincipal();
            Long maxAge = maximumAuthenticationAge(request);
            String prompt = text(request.getAdditionalParameters().get("prompt"));
            boolean freshRequired = request.getScopes().contains("openid") && principal.isAuthenticated()
                    && !(principal instanceof AnonymousAuthenticationToken)
                    && (prompt != null && List.of(prompt.split(" ", -1)).contains("login")
                        || maxAge != null && authenticationTooOld(principal, maxAge, clock.instant()));
            ServletRequestAttributes servlet = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes value
                    ? value : null;
            boolean provenRequest = freshRequired && servlet != null && principal.getDetails() instanceof Map<?, ?> details
                    && details.get("reauthenticationExpiresAt") instanceof Instant expiresAt && expiresAt.isAfter(clock.instant())
                    && TenantLoginAuthenticationFilter.authorizationRequestDigest(realm.getIssuer(), servlet.getRequest().getParameterMap())
                            .equals(details.get("reauthenticatedRequest"));
            if (freshRequired && !provenRequest) {
                Authentication anonymous = new AnonymousAuthenticationToken("oauthReauthentication", "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
                OAuth2AuthorizationCodeRequestAuthenticationToken unauthenticated =
                        new OAuth2AuthorizationCodeRequestAuthenticationToken(request.getAuthorizationUri(), request.getClientId(),
                                anonymous, request.getRedirectUri(), request.getState(), request.getScopes(), request.getAdditionalParameters());
                Authentication result = delegate.authenticate(unauthenticated);
                if (!result.isAuthenticated()) {
                    SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
                }
                return result;
            }
            Authentication result = delegate.authenticate(authentication);
            if (provenRequest && result.isAuthenticated() && principal instanceof UsernamePasswordAuthenticationToken user) {
                Map<String, Object> details = new java.util.HashMap<>();
                ((Map<?, ?>) user.getDetails()).forEach((key, value) -> {
                    if (key instanceof String name && !name.equals("reauthenticatedRequest")
                            && !name.equals("reauthenticationExpiresAt")) { details.put(name, value); }
                });
                user.setDetails(details);
                new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(),
                        servlet.getRequest(), servlet.getResponse());
            }
            return result;
        };
    }

    private static Long maximumAuthenticationAge(OAuth2AuthorizationCodeRequestAuthenticationToken request) {
        Object value = request.getAdditionalParameters().get("max_age");
        if (value == null) { return null; }
        if (!(value instanceof String text) || !text.matches("0|[1-9][0-9]{0,4}") || Long.parseLong(text) > 86400L) {
            throw new OAuth2AuthorizationCodeRequestAuthenticationException(
                    new OAuth2Error("invalid_request", "Request rejected", null), null);
        }
        return Long.parseLong(text);
    }

    private static boolean authenticationTooOld(Authentication principal, long maxAge, Instant now) {
        return !(principal.getDetails() instanceof Map<?, ?> details)
                || !(details.get("authTime") instanceof Instant authenticatedAt)
                || authenticatedAt.isAfter(now) || maxAge == 0L || !authenticatedAt.plusSeconds(maxAge).isAfter(now);
    }

    public static GrantTransaction userInfo() {
        return (authentication, delegate, realm, service) -> delegate.authenticate(authentication);
    }

    /** Projects only the original SAS-validated JWT after all original authorization guards complete. */
    public static GrantTransaction userInfo(@Qualifier("tokenUsageAuditComponent") TokenUsageAuditComponent producer,
            @Qualifier("tokenUsageAuditEventConverterImpl") TokenUsageAuditEventConverter converter,
            @Qualifier("oauthUsageObservationProperties") OAuthUsageObservationProperties properties, URI resource,
            @Qualifier("idpClock") Clock clock, @Qualifier("defaultValidator") Validator validator) {
        if (!properties.isEnabled()) { return userInfo(); }
        Objects.requireNonNull(producer, "producer"); Objects.requireNonNull(converter, "converter");
        Objects.requireNonNull(resource, "resource"); Objects.requireNonNull(clock, "clock");
        ValidationUtils schema = new ValidationUtils(Objects.requireNonNull(validator, "validator"));
        schema.validate(properties);
        return (authentication, delegate, realm, service) -> {
            Authentication result = delegate.authenticate(authentication);
            if (result == null || !result.isAuthenticated()) { return result; }
            try {
                if (!(authentication.getPrincipal() instanceof JwtAuthenticationToken validated)
                        || !realm.getIssuer().toASCIIString().equals(validated.getToken().getClaimAsString("iss"))) {
                    throw new IllegalStateException("OAUTH_USERINFO_VERIFIED_FACT_REQUIRED");
                }
                var proof = schema.validate(converter.toProof(validated.getToken(), realm.getIssuer(), resource, producer));
                String trace = MDC.get("traceId");
                if (trace != null && !trace.matches("[A-Za-z0-9._~-]{1,128}")) { trace = null; }
                var context = TokenAuditContextBO.builder().proof(proof).fallbackIssuer(properties.getFallbackIssuer())
                        .resource(resource).eventId(java.util.UUID.randomUUID().toString().replace("-", ""))
                        .result("SUCCESS").reasonCode("TOKEN_USE_VERIFIED").occurredAt(clock.instant()).traceId(trace).build();
                producer.appendStrict(schema.validate(converter.toTarget(schema.validate(context))));
                return result;
            } catch (RuntimeException unavailable) {
                throw new UserInfoUsageUnavailableException(unavailable);
            }
        };
    }

    /** Identifies an already-attempted handoff so the error renderer cannot invent a second decision. */
    public static final class UserInfoUsageUnavailableException extends OAuth2AuthenticationException {
        public UserInfoUsageUnavailableException(Throwable cause) {
            super(new OAuth2Error("temporarily_unavailable", "Request temporarily unavailable", null), cause);
        }
    }

    public static GrantTransaction authorizationConsent(OAuth2AuthorizationService authorizations) {
        Objects.requireNonNull(authorizations, "authorizations");
        return new GrantTransaction() {
            @Override
            public Authentication authenticate(Authentication authentication, AuthenticationProvider delegate,
                    top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO realm,
                    OAuthTokenIssueService service) {
                OAuth2AuthorizationConsentAuthenticationToken consent =
                        (OAuth2AuthorizationConsentAuthenticationToken) authentication;
                if (!"deny".equals(consent.getAdditionalParameters().get("consent_action")) && !consent.getScopes().isEmpty()) {
                    return delegate.authenticate(authentication);
                }
                OAuth2Authorization pending = authorizations.findByToken(consent.getState(), new OAuth2TokenType("state"));
                Authentication principal = (Authentication) consent.getPrincipal();
                OAuth2AuthorizationRequest request = pending == null ? null
                        : pending.getAttribute(OAuth2AuthorizationRequest.class.getName());
                if (request == null || !principal.isAuthenticated()
                        || !pending.getPrincipalName().equals(principal.getName())
                        || !request.getClientId().equals(consent.getClientId())) {
                    throw new OAuth2AuthorizationCodeRequestAuthenticationException(
                            new OAuth2Error("invalid_request", "Request rejected", null), null);
                }
                authorizations.remove(pending);
                OAuth2AuthorizationCodeRequestAuthenticationToken deniedRequest =
                        new OAuth2AuthorizationCodeRequestAuthenticationToken(request.getAuthorizationUri(),
                                request.getClientId(), principal, request.getRedirectUri(), request.getState(),
                                request.getScopes(), Map.of());
                throw new OAuth2AuthorizationCodeRequestAuthenticationException(
                        new OAuth2Error("access_denied", "Request rejected", null), deniedRequest);
            }

            @Override
            public boolean commitsConsentDenial() {
                return true;
            }
        };
    }

    public static GrantTransaction authorizationCode() {
        return (authentication, delegate, realm, service) -> {
            if (!(authentication instanceof OAuth2AuthorizationCodeAuthenticationToken code)) {
                throw new IllegalArgumentException("OAUTH_AUTHORIZATION_CODE_GRANT_REQUIRED");
            }
            RegisteredClient client = registeredClient(code.getPrincipal());
            return service.consumeAndIssue(realm, ConsumeOAuthTokenCommand.builder()
                    .tokenDigest(Digests.sha256Hex(code.getCode()))
                    .clientPk(parseClientPk(client))
                    .tokenType(OAuthTokenTypeEnum.CODE)
                    .verifier(text(code.getAdditionalParameters().get("code_verifier")))
                    .redirectUri(uri(code.getRedirectUri()))
                    .build(), () -> delegate.authenticate(authentication));
        };
    }

    public static GrantTransaction refreshToken() {
        return (authentication, delegate, realm, service) -> {
            if (!(authentication instanceof OAuth2RefreshTokenAuthenticationToken refresh)) {
                throw new IllegalArgumentException("OAUTH_REFRESH_TOKEN_GRANT_REQUIRED");
            }
            RegisteredClient client = registeredClient(refresh.getPrincipal());
            return service.consumeAndIssue(realm, ConsumeOAuthTokenCommand.builder()
                    .tokenDigest(Digests.sha256Hex(refresh.getRefreshToken()))
                    .clientPk(parseClientPk(client))
                    .tokenType(OAuthTokenTypeEnum.REFRESH)
                    .scopes(refresh.getScopes())
                    .build(), () -> delegate.authenticate(authentication));
        };
    }

    public static GrantTransaction clientCredentials() {
        return (authentication, delegate, realm, service) -> {
            if (!(authentication instanceof OAuth2ClientCredentialsAuthenticationToken clientCredentials)) {
                throw new IllegalArgumentException("OAUTH_CLIENT_CREDENTIALS_GRANT_REQUIRED");
            }
            RegisteredClient client = registeredClient(clientCredentials.getPrincipal());
            Authentication issued = delegate.authenticate(authentication);
            if (issued != null) {
                Object resource = clientCredentials.getAdditionalParameters().get("resource");
                URI resourceUri = resource instanceof URI uri
                        ? uri : resource instanceof String value ? URI.create(value) : null;
                if (resourceUri == null) {
                    throw new IllegalArgumentException("OAUTH_TOKEN_RESOURCE_REQUIRED");
                }
                service.recordClientCredentialsIssued(realm, parseClientPk(client), resourceUri);
            }
            return issued;
        };
    }

    @FunctionalInterface
    public interface GrantTransaction {
        default boolean commitsConsentDenial() {
            return false;
        }

        Authentication authenticate(
                Authentication authentication,
                AuthenticationProvider delegate,
                top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO trustedRealm,
                OAuthTokenIssueService tokenIssueService);
    }

    private static RegisteredClient registeredClient(Object principal) {
        if (principal instanceof OAuth2ClientAuthenticationToken client && client.getRegisteredClient() != null) {
            return client.getRegisteredClient();
        }
        throw new IllegalArgumentException("OAUTH_CLIENT_AUTHENTICATION_REQUIRED");
    }

    private static long parseClientPk(RegisteredClient client) {
        try {
            long id = Long.parseLong(client.getId());
            if (id <= 0L) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("OAUTH_CLIENT_REGISTRATION_ID_INVALID", failure);
        }
    }

    private static String text(Object value) {
        return value instanceof String text ? text : null;
    }

    private static URI uri(String value) {
        return value == null || value.isBlank() ? null : URI.create(value);
    }
}
