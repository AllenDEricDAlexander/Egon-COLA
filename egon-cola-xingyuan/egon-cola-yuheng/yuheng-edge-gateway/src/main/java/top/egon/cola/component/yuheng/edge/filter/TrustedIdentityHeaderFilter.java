package top.egon.cola.component.yuheng.edge.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component("trustedIdentityHeaderFilter")
@Slf4j
public class TrustedIdentityHeaderFilter implements GlobalFilter, Ordered {
    private static final String BFF_COOKIE = "__Host-egon_bff";
    private static final Set<String> UNTRUSTED_IDENTITY_HEADERS = Set.of(
            "tenant_id",
            "user_id",
            "scope",
            "x-tenant-id",
            "x-user-id",
            "x-scopes",
            "x-egon-tenant-id",
            "x-egon-user-id",
            "x-egon-scopes");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> UNTRUSTED_IDENTITY_HEADERS.forEach(headers::remove))
                .build();
        ServerWebExchange sanitized = exchange.mutate().request(request).build();
        if (!isApiPath(request.getPath().value())) {
            return chain.filter(sanitized);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean bearer = authorization != null
                && authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length());
        boolean hasSession = request.getCookies().containsKey(BFF_COOKIE);
        if (bearer && hasSession) {
            return writeError(sanitized, HttpStatus.BAD_REQUEST, "invalid_request");
        }
        if (bearer) {
            return chain.filter(sanitized);
        }
        return sanitized.getPrincipal()
                .ofType(Authentication.class)
                .filter(TrustedIdentityHeaderFilter::authenticated)
                .hasElement()
                .flatMap(hasAuthenticatedSession -> hasAuthenticatedSession
                        ? chain.filter(sanitized)
                        : writeError(sanitized, HttpStatus.UNAUTHORIZED, "invalid_token"));
    }

    @Override
    public int getOrder() {
        return -1_000;
    }

    private static boolean isApiPath(String path) {
        return path.startsWith("/api/") || path.equals("/api")
                || path.startsWith("/mcp/") || path.equals("/mcp");
    }

    private static boolean authenticated(Authentication authentication) {
        return authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null
                && !authentication.getName().isBlank();
    }

    private Mono<Void> writeError(
            ServerWebExchange exchange,
            HttpStatus status,
            String code) {
        log.debug("Rejected Yuheng route request; status={}, error={}", status.value(), code);
        byte[] body = ("{\"error\":\"" + code + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(body)));
    }
}
