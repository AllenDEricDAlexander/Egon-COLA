package top.egon.cola.component.yuheng.edge.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.TokenRelayGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.edge.service.domain.enums.EdgeSessionBindingStateEnum;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeAuthorizedClientRepository;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeSessionBindingComponent;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;

/** Selects one configured resource and delegates token relay with its explicit real registration. */
@Slf4j
@Component("edgeResourceAuthorizationGatewayFilterFactory")
@RequiredArgsConstructor
public class EdgeResourceAuthorizationGatewayFilterFactory extends AbstractGatewayFilterFactory<AbstractGatewayFilterFactory.NameConfig> {
    public static final String VERIFIED_ACCESS = EdgeResourceAuthorizationGatewayFilterFactory.class.getName() + ".verifiedAccess";
    public static final String EXTERNAL_VERIFIED_ACCESS = EdgeResourceAuthorizationGatewayFilterFactory.class.getName() + ".externalVerifiedAccess";
    public static final String AUTHENTICATION_OBSERVATION = EdgeResourceAuthorizationGatewayFilterFactory.class.getName() + ".authenticationObservation";
    public static final String AUTHORIZATION_OBSERVATION = EdgeResourceAuthorizationGatewayFilterFactory.class.getName() + ".authorizationObservation";
    private static final String VERIFICATION_ATTEMPTED = EdgeResourceAuthorizationGatewayFilterFactory.class.getName() + ".verificationAttempted";
    private static final String FORWARDED = EdgeResourceAuthorizationGatewayFilterFactory.class.getName() + ".forwarded";
    @Qualifier("edgeSessionBindingComponent") private final EdgeSessionBindingComponent bindings;
    @Qualifier("edgeAuthorizedClientRepository") private final EdgeAuthorizedClientRepository clients;
    @Qualifier("tokenRelayGatewayFilterFactory") private final TokenRelayGatewayFilterFactory tokenRelayGatewayFilterFactory;
    @Qualifier("jacksonObjectMapper") private final ObjectMapper mapper;
    private final AntPathMatcher paths = new AntPathMatcher();

    @Override public Class<NameConfig> getConfigClass() { return NameConfig.class; }
    @Override public List<String> shortcutFieldOrder() { return List.of("name"); }

    @Override
    public GatewayFilter apply(NameConfig config) {
        String alias = Objects.requireNonNull(config.getName(), "resource alias");
        bindings.resource(alias);
        return new OrderedGatewayFilter((exchange, chain) -> Mono.usingWhen(Mono.fromSupplier(() -> Map.of(
                AUTHENTICATION_OBSERVATION, new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO(),
                AUTHORIZATION_OBSERVATION, new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO())), states -> Mono.defer(() -> {
            exchange.getAttributes().putAll(states);
            var resource = bindings.resource(alias);
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if (route == null || !alias.equals(route.getMetadata().get("edge-registration-id"))
                    || !resource.getRouteIds().contains(route.getId()) || !routeUri(resource.getResourceUri()).equals(route.getUri())
                    || resource.getRequestPaths().stream().noneMatch(pattern -> paths.match(pattern, exchange.getRequest().getPath().value()))) {
                return error(exchange, 503, "temporarily_unavailable");
            }
            List<String> headers = exchange.getRequest().getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
            if (headers.size() > 1) { return error(exchange, 400, "invalid_request"); }
            boolean bearer = headers.size() == 1 && headers.getFirst().regionMatches(true, 0, "Bearer ", 0, 7);
            if (bearer && exchange.getRequest().getCookies().containsKey("__Host-egon_bff")) { return error(exchange, 400, "invalid_request"); }
            if (bearer) {
                String token = headers.getFirst().substring(7).trim();
                if (token.isEmpty() || token.length() > 8192 || token.contains(",")) { return error(exchange, 401, "invalid_token"); }
                return bindings.verifyExternal(alias, token, UUID.randomUUID().toString().replace("-", "")).flatMap(result -> {
                    exchange.getAttributes().put(VERIFICATION_ATTEMPTED, Boolean.TRUE);
                    if (result instanceof AccessTokenVerification.Valid<?> valid && valid.proof() != null) {
                        exchange.getAttributes().put(EXTERNAL_VERIFIED_ACCESS, valid);
                        return chain.filter(exchange).doFinally(signal -> exchange.getAttributes().remove(EXTERNAL_VERIFIED_ACCESS));
                    }
                    return error(exchange, result instanceof AccessTokenVerification.Unavailable<?> ? 503 : 401,
                            result instanceof AccessTokenVerification.Unavailable<?> ? "temporarily_unavailable" : "invalid_token");
                });
            }
            return exchange.getPrincipal().ofType(Authentication.class).switchIfEmpty(Mono.error(
                    new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"))))
                    .flatMap(authentication -> bindings.load(exchange).flatMap(binding -> {
                        if (binding.getState() != EdgeSessionBindingStateEnum.ACTIVE || !authentication.getName().equals(binding.getSubject())) {
                            return error(exchange, 401, "invalid_token");
                        }
                        String id = bindings.resolveRegistration(alias, binding.getIssuer());
                        exchange.getAttributes().put(EdgeSessionBindingComponent.RESOURCE_ALIAS, alias);
                        exchange.getAttributes().put(EdgeSessionBindingComponent.REGISTRATION_ID, id);
                        exchange.getAttributes().put(EdgeSessionBindingComponent.ISSUER, binding.getIssuer().toASCIIString());
                        exchange.getAttributes().put(EdgeSessionBindingComponent.RESOURCE, resource.getResourceUri().toASCIIString());
                        exchange.getAttributes().put(EdgeSessionBindingComponent.GENERATION, binding.getGeneration());
                        return clients.loadAuthorizedClient(id, authentication, exchange)
                                .switchIfEmpty(Mono.error(new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("resource_authorization_required"))))
                                .flatMap(client -> {
                            var relay = new NameConfig(); relay.setName(id);
                            return tokenRelayGatewayFilterFactory.apply(relay).filter(exchange, relayed ->
                                    clients.loadAuthorizedClient(id, authentication, relayed)
                                            .switchIfEmpty(Mono.error(new OAuth2AuthenticationException(new org.springframework.security.oauth2.core.OAuth2Error("resource_authorization_required"))))
                                            .flatMap(current ->
                                            clients.verifyForForward(current, authentication, relayed))
                                            .flatMap(verified -> {
                                                relayed.getAttributes().put(VERIFICATION_ATTEMPTED, Boolean.TRUE);
                                                if (!(verified instanceof AccessTokenVerification.Valid<IdentityPrincipal> valid) || valid.proof() == null) {
                                                    return error(relayed, verified instanceof AccessTokenVerification.Unavailable<?> ? 503 : 401,
                                                            verified instanceof AccessTokenVerification.Unavailable<?> ? "temporarily_unavailable" : "invalid_token");
                                                }
                                                return bindings.load(relayed).flatMap(active -> {
                                                    if (active.getGeneration() != binding.getGeneration() || active.getState() != EdgeSessionBindingStateEnum.ACTIVE) {
                                                        return error(relayed, 401, "invalid_token");
                                                    }
                                                    relayed.getAttributes().put(VERIFIED_ACCESS, valid);
                                                    relayed.getAttributes().put(FORWARDED, Boolean.TRUE);
                                                    return chain.filter(relayed);
                                                });
                                            }));
                        });
                    })).onErrorResume(OAuth2AuthenticationException.class, failure -> {
                        if (Boolean.TRUE.equals(exchange.getAttribute(FORWARDED))) { return Mono.error(failure); }
                        String code = failure.getError().getErrorCode();
                        return error(exchange, "temporarily_unavailable".equals(code) ? 503 : "invalid_request".equals(code) ? 400 : 401,
                                "temporarily_unavailable".equals(code) || "invalid_request".equals(code) || "resource_authorization_required".equals(code) ? code : "invalid_token");
                    }).onErrorResume(org.springframework.security.oauth2.core.OAuth2AuthorizationException.class, failure -> {
                        if (Boolean.TRUE.equals(exchange.getAttribute(FORWARDED))) { return Mono.error(failure); }
                        String code = failure.getError().getErrorCode();
                        return error(exchange, "invalid_grant".equals(code) ? 401 : 503, "invalid_grant".equals(code) ? "resource_authorization_required" : "temporarily_unavailable");
                    }).doFinally(signal -> {
                        exchange.getAttributes().remove(VERIFIED_ACCESS); exchange.getAttributes().remove(FORWARDED);
                        clients.clearRequestVerification(exchange);
                        for (String key : List.of(EdgeSessionBindingComponent.RESOURCE_ALIAS, EdgeSessionBindingComponent.REGISTRATION_ID,
                                EdgeSessionBindingComponent.ISSUER, EdgeSessionBindingComponent.RESOURCE, EdgeSessionBindingComponent.GENERATION)) {
                            exchange.getAttributes().remove(key);
                        }
                    });
        }).contextWrite(context -> context.put(top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO.class,
                states.get(AUTHORIZATION_OBSERVATION))), states -> closeObservation(exchange, states),
                (states, failure) -> closeObservation(exchange, states), states -> closeObservation(exchange, states)), -1100);
    }

    private Mono<Void> closeObservation(ServerWebExchange exchange,
            Map<String, top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO> states) {
        return Mono.fromRunnable(() -> states.values().forEach(top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO::close))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).then()
                .doFinally(signal -> {
                    states.forEach((key, value) -> exchange.getAttributes().remove(key, value));
                    exchange.getAttributes().remove(VERIFICATION_ATTEMPTED);
                    exchange.getAttributes().remove(EXTERNAL_VERIFIED_ACCESS); exchange.getAttributes().remove(VERIFIED_ACCESS);
                    clients.clearRequestVerification(exchange);
                });
    }

    private static java.net.URI routeUri(java.net.URI resource) {
        // Route.AbstractBuilder inserts HTTPS port 443; the token audience keeps the exact catalog URI.
        return resource.getPort() < 0 ? org.springframework.web.util.UriComponentsBuilder.fromUri(resource).port(443).build(false).toUri() : resource;
    }

    private Mono<Void> error(ServerWebExchange exchange, int status, String code) {
        if (status == 401 && !Boolean.TRUE.equals(exchange.getAttribute(VERIFICATION_ATTEMPTED))) {
            var marker = exchange.<top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO>getAttribute(AUTHENTICATION_OBSERVATION);
            if (marker == null) { return writeError(exchange, 503, "temporarily_unavailable"); }
            return bindings.authenticationFailure(marker).flatMap(confirmed -> writeError(exchange, confirmed ? status : 503, confirmed ? code : "temporarily_unavailable"));
        }
        return writeError(exchange, status, code);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, int status, String code) {
        return Mono.defer(() -> {
            exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
            exchange.getResponse().getHeaders().setCacheControl("no-store");
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            if (status == 401) { exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer"); }
            return Mono.fromCallable(() -> mapper.writeValueAsBytes(Map.of("error", code)))
                    .flatMap(body -> exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body))));
        });
    }
}
