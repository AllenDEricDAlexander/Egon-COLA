package top.egon.cola.component.yuheng.edge.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeSessionBindingComponent;
import top.egon.cola.component.yuheng.edge.service.domain.enums.EdgeSessionBindingStateEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.util.Map;
import java.util.Set;

@Component("trustedIdentityHeaderFilter")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TrustedIdentityHeaderFilter implements GlobalFilter, Ordered {
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("edgeSessionBindingComponent")
    private final EdgeSessionBindingComponent bindings;

    public TrustedIdentityHeaderFilter(ObjectMapper objectMapper) { this(objectMapper, null); }

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
        boolean configuredPath = bindings != null && bindings.resourcesContainPath(request.getPath().value());
        if (!isApiPath(request.getPath().value()) && !configuredPath) {
            return chain.filter(sanitized);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean bearer = authorization != null
                && authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length());
        boolean hasSession = request.getCookies().containsKey(BFF_COOKIE);
        Object verified = sanitized.getAttribute(EdgeResourceAuthorizationGatewayFilterFactory.VERIFIED_ACCESS);
        if (bearer && hasSession && verified == null) {
            return writeError(sanitized, HttpStatus.BAD_REQUEST, "invalid_request");
        }
        ServerWebExchange forwarded = sanitized.mutate().request(request.mutate()
                .headers(headers -> headers.remove(HttpHeaders.COOKIE)).build()).build();
        if (bearer && verified == null) {
            Object external = sanitized.getAttribute(EdgeResourceAuthorizationGatewayFilterFactory.EXTERNAL_VERIFIED_ACCESS);
            if (external instanceof AccessTokenVerification.Valid<?> valid && valid.proof() != null) {
                var identity = valid.principal();
                var trusted = forwarded.mutate().request(forwarded.getRequest().mutate().headers(headers -> {
                    if (identity.tenantId() != null) { headers.set("tenant_id", identity.tenantId()); }
                    headers.set("user_id", identity.subject()); headers.set("scope", String.join(" ", valid.scopes()));
                }).build()).build();
                return chain.filter(trusted);
            }
            return chain.filter(forwarded);
        }
        if (bindings == null || !(verified instanceof AccessTokenVerification.Valid<?> valid)
                || !(valid.principal() instanceof IdentityPrincipal identity) || valid.proof() == null) {
            return writeError(sanitized, HttpStatus.UNAUTHORIZED, "invalid_token");
        }
        var entered = new java.util.concurrent.atomic.AtomicBoolean();
        return bindings.load(sanitized).flatMap(binding -> {
            String alias = sanitized.getAttribute(EdgeSessionBindingComponent.RESOURCE_ALIAS);
            String id = sanitized.getAttribute(EdgeSessionBindingComponent.REGISTRATION_ID);
            Number generation = sanitized.getAttribute(EdgeSessionBindingComponent.GENERATION);
            if (binding.getState() != EdgeSessionBindingStateEnum.ACTIVE || generation == null || generation.longValue() != binding.getGeneration()
                    || !identity.subject().equals(binding.getSubject()) || !identity.tenantId().equals(binding.getTenantId())
                    || !valid.proof().issuer().equals(binding.getIssuer()) || alias == null || id == null
                    || !id.equals(bindings.resolveRegistration(alias, binding.getIssuer()))
                    || !valid.proof().resource().equals(bindings.resource(alias).getResourceUri())) {
                return writeError(sanitized, HttpStatus.UNAUTHORIZED, "invalid_token");
            }
            var trusted = forwarded.mutate().request(forwarded.getRequest().mutate().headers(headers -> {
                headers.set("tenant_id", identity.tenantId()); headers.set("user_id", identity.subject());
                headers.set("scope", String.join(" ", valid.scopes()));
            }).build()).build();
            entered.set(true);
            return chain.filter(trusted);
        }).onErrorResume(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class,
                failure -> entered.get() ? Mono.error(failure) : writeError(sanitized, "temporarily_unavailable".equals(failure.getError().getErrorCode())
                        ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.UNAUTHORIZED,
                        "temporarily_unavailable".equals(failure.getError().getErrorCode()) ? "temporarily_unavailable" : "invalid_token"));
    }

    @Override
    public int getOrder() {
        return -1_000;
    }

    private static boolean isApiPath(String path) {
        return path.startsWith("/api/") || path.equals("/api")
                || path.startsWith("/mcp/") || path.equals("/mcp");
    }

    private Mono<Void> writeError(
            ServerWebExchange exchange,
            HttpStatus status,
            String code) {
        log.debug("Rejected Yuheng route request; status={}, error={}", status.value(), code);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        if (status == HttpStatus.UNAUTHORIZED) { exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer"); }
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(Map.of("error", code)))
                .flatMap(body -> exchange.getResponse().writeWith(Mono.just(
                        exchange.getResponse().bufferFactory().wrap(body))));
    }
}
