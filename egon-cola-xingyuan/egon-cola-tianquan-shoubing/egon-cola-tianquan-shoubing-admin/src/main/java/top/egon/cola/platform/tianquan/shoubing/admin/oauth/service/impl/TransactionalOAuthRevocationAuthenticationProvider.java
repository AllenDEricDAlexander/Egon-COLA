package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.RevokeOAuthTokenCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;

/** Keeps SAS token-revocation checks and the durable security event in one transaction. */
@RequiredArgsConstructor
@Slf4j
public final class TransactionalOAuthRevocationAuthenticationProvider implements AuthenticationProvider {

    @Qualifier("tenantAuthorizationService")
    private final OAuth2AuthorizationService authorizations;
    @Qualifier("tokenRevocationService")
    private final TokenRevocationService revocations;
    @Qualifier("tenantIssuerService")
    private final TenantIssuerService issuers;
    @Qualifier("transactionManager")
    private final PlatformTransactionManager transactionManager;

    @Override
    public Authentication authenticate(Authentication authentication) throws OAuth2AuthenticationException {
        OAuth2TokenRevocationAuthenticationToken request = (OAuth2TokenRevocationAuthenticationToken) authentication;
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setName("oauthTokenRevocation");
        return transaction.execute(status -> {
            OAuth2TokenRevocationAuthenticationProvider delegate =
                    new OAuth2TokenRevocationAuthenticationProvider(authorizations);
            Object principal = request.getPrincipal();
            if (!(principal instanceof OAuth2ClientAuthenticationToken clientAuthentication)
                    || !clientAuthentication.isAuthenticated()
                    || clientAuthentication.getRegisteredClient() == null) {
                return delegate.authenticate(request);
            }
            OAuth2Authorization authorization = authorizations.findByToken(request.getToken(), null);
            Authentication result = delegate.authenticate(request);
            if (authorization == null || !(result instanceof OAuth2TokenRevocationAuthenticationToken)
                    || !result.isAuthenticated()) {
                return result;
            }

            long clientPk = positiveClientId(clientAuthentication.getRegisteredClient().getId());
            TrustedRealmBO trustedRealm = issuers.resolveCurrentIssuer();
            revocations.revoke(trustedRealm, RevokeOAuthTokenCommand.builder()
                    .tokenDigest(Digests.sha256Hex(request.getToken()))
                    .clientPk(clientPk)
                    .reasonCode("RFC7009_REVOKE")
                    .build());
            return result;
        });
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2TokenRevocationAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static long positiveClientId(String value) {
        try {
            long clientPk = Long.parseLong(value);
            if (clientPk > 0L) {
                return clientPk;
            }
        } catch (NumberFormatException invalid) {
            throw new IllegalStateException("OAUTH_REVOCATION_CLIENT_ID_INVALID", invalid);
        }
        throw new IllegalStateException("OAUTH_REVOCATION_CLIENT_ID_INVALID");
    }
}
