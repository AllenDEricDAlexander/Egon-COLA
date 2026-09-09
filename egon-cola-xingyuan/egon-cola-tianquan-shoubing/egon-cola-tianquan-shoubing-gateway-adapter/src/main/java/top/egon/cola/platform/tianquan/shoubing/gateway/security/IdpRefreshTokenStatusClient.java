package top.egon.cola.platform.tianquan.shoubing.gateway.security;

import reactor.core.publisher.Mono;
import top.egon.cola.platform.tianquan.shoubing.core.token.RefreshTokenStatus;

/**
 * Calls the Tianquan-Shoubing internal endpoint that validates a USER refresh token.
 */
public interface IdpRefreshTokenStatusClient {

    Mono<Response> validate(String refreshToken);

    record Response(int status, RefreshTokenStatus tokenStatus) {
    }
}
