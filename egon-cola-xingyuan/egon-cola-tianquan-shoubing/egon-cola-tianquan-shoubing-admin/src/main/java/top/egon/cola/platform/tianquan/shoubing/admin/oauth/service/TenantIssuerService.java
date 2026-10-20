package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;

@Validated
public interface TenantIssuerService {

    TrustedRealmBO resolveExactIssuer(@NotNull URI issuer);

    default TrustedRealmBO resolveCurrentIssuer() {
        AuthorizationServerContext context = AuthorizationServerContextHolder.getContext();
        if (context == null || context.getIssuer() == null || context.getIssuer().isBlank()) {
            throw invalidIssuer();
        }
        try {
            return resolveExactIssuer(URI.create(context.getIssuer()));
        } catch (IllegalArgumentException failure) {
            throw invalidIssuer();
        }
    }

    private static OAuth2AuthenticationException invalidIssuer() {
        return new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "Request rejected", null));
    }
}
