package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import java.net.URI;
import java.util.Objects;
import org.redisson.client.RedisException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataAccessException;
import org.redisson.client.RedisException;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command.ConsumeOAuthTokenCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthTokenIssueService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;

public final class TransactionalOAuthAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider delegate;
    private final TransactionTemplate transactionTemplate;
    private final OAuthTokenIssueService tokenIssueService;
    private final TenantIssuerService tenantIssuerService;
    private final GrantTransaction grantTransaction;

    public TransactionalOAuthAuthenticationProvider(
            AuthenticationProvider delegate,
            TransactionTemplate transactionTemplate,
            OAuthTokenIssueService tokenIssueService,
            TenantIssuerService tenantIssuerService,
            GrantTransaction grantTransaction) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
        this.tokenIssueService = Objects.requireNonNull(tokenIssueService, "tokenIssueService");
        this.tenantIssuerService = Objects.requireNonNull(tenantIssuerService, "tenantIssuerService");
        this.grantTransaction = Objects.requireNonNull(grantTransaction, "grantTransaction");
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        try {
            return transactionTemplate.execute(status -> {
                var trustedRealm = tenantIssuerService.resolveCurrentIssuer();
                return grantTransaction.authenticate(authentication, delegate, trustedRealm, tokenIssueService);
            });
        } catch (DataAccessException | RedisException failure) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("temporarily_unavailable", "Request temporarily unavailable", null), failure);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
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
