package top.egon.cola.platform.idp.gateway.security;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Internal IdP refresh transport used only by Gateway credential recovery.
 */
public interface IdpRefreshClient {

    Mono<Response> refresh(String refreshToken);

    record Response(int status, Map<String, List<String>> headers) {
        public Response {
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }
    }
}
