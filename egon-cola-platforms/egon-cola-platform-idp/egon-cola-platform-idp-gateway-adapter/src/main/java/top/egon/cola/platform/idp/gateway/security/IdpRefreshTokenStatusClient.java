package top.egon.cola.platform.idp.gateway.security;

import reactor.core.publisher.Mono;
import top.egon.cola.platform.idp.core.token.RefreshTokenStatus;

/**
 * Calls the IdP internal endpoint that validates a USER refresh token.
 */
public interface IdpRefreshTokenStatusClient {

    Mono<Response> validate(String refreshToken);

    record Response(int status, RefreshTokenStatus tokenStatus) {
    }
}
