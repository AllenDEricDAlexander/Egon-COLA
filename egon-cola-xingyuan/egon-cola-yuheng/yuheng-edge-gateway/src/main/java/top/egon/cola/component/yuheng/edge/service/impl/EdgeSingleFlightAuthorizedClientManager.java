package top.egon.cola.component.yuheng.edge.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.yuheng.edge.service.domain.enums.EdgeSessionBindingStateEnum;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class EdgeSingleFlightAuthorizedClientManager implements ReactiveOAuth2AuthorizedClientManager {
    private static final String LOCK_PREFIX = "egon:bff:oauth-refresh:v2:";
    private static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";

    @Qualifier("edgeDefaultAuthorizedClientManager") private final ReactiveOAuth2AuthorizedClientManager delegate;
    @Qualifier("reactiveClientRegistrationRepository") private final ReactiveClientRegistrationRepository registrations;
    @Qualifier("redissonReactiveClient") private final RedissonReactiveClient redisson;
    @Qualifier("edgeOAuthProperties") private final EdgeOAuthProperties properties;
    @Qualifier("edgeSessionBindingComponent") private final EdgeSessionBindingComponent bindings;
    @Qualifier("edgeAuthorizedClientRepository") private final EdgeAuthorizedClientRepository clients;

    public EdgeSingleFlightAuthorizedClientManager(ReactiveOAuth2AuthorizedClientManager delegate,
            ReactiveClientRegistrationRepository registrations, RedissonReactiveClient redisson, EdgeOAuthProperties properties) {
        this(delegate, registrations, redisson, properties, null, null);
    }

    public static EdgeSingleFlightAuthorizedClientManager createConfigured(ReactiveOAuth2AuthorizedClientManager delegate,
            ReactiveClientRegistrationRepository registrations, RedissonReactiveClient redisson, EdgeOAuthProperties properties,
            EdgeSessionBindingComponent bindings, EdgeAuthorizedClientRepository clients) {
        return new EdgeSingleFlightAuthorizedClientManager(java.util.Objects.requireNonNull(delegate), java.util.Objects.requireNonNull(registrations),
                java.util.Objects.requireNonNull(redisson), java.util.Objects.requireNonNull(properties), java.util.Objects.requireNonNull(bindings), java.util.Objects.requireNonNull(clients));
    }

    @Override
    public Mono<org.springframework.security.oauth2.client.OAuth2AuthorizedClient> authorize(
            OAuth2AuthorizeRequest authorizeRequest) {
        if (bindings == null || clients == null) { return Mono.error(unavailable("Edge authoritative refresh adapter is required")); }
        if (authorizeRequest == null
                || authorizeRequest.getClientRegistrationId() == null
                || authorizeRequest.getPrincipal() == null) {
            return Mono.error(unavailable("Edge refresh identity is unavailable"));
        }
        return exchange(authorizeRequest)
                .flatMap(exchange -> registrations.findByRegistrationId(authorizeRequest.getClientRegistrationId())
                        .switchIfEmpty(Mono.error(unavailable("Edge refresh registration is unavailable")))
                        .flatMap(registration -> withSessionLock(
                                authorizeRequest,
                                exchange,
                                registration)))
                .doOnError(OAuth2AuthorizationException.class, exception -> log.debug(
                        "Edge OAuth2 authorization failed; registrationId={}, errorCode={}",
                        authorizeRequest.getClientRegistrationId(),
                        exception.getError().getErrorCode()));
    }

    private Mono<org.springframework.security.oauth2.client.OAuth2AuthorizedClient> withSessionLock(
            OAuth2AuthorizeRequest request, ServerWebExchange exchange, ClientRegistration registration) {
        return bindings.withLifecycle(exchange, lifecycle -> bindings.state(lifecycle).flatMap(binding -> {
            String alias = bindings.aliasForRegistration(registration.getRegistrationId());
            if (binding.getState() != EdgeSessionBindingStateEnum.ACTIVE || !binding.getSubject().equals(request.getPrincipal().getName())
                    || !registration.getRegistrationId().equals(bindings.resolveRegistration(alias, binding.getIssuer()))) {
                return Mono.error(unavailable("Edge refresh does not match the active canonical resource tuple"));
            }
            String selected = exchange.getAttribute(EdgeSessionBindingComponent.REGISTRATION_ID);
            if (selected != null && !selected.equals(registration.getRegistrationId())) {
                return Mono.error(unavailable("Edge refresh selection has changed"));
            }
            exchange.getAttributes().put(EdgeSessionBindingComponent.REGISTRATION_ID, registration.getRegistrationId());
            exchange.getAttributes().put(EdgeSessionBindingComponent.RESOURCE_ALIAS, alias);
            String lockName = LOCK_PREFIX + "{" + binding.getBindingId() + "}:" + EdgeSessionBindingComponent.digest(binding.getBindingId(), binding.getIssuer().toASCIIString(),
                    binding.getTenantId(), registration.getClientId(), registration.getRegistrationId(), bindings.resource(alias).getResourceUri().toASCIIString(), binding.getSubject());
            var nativeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(request.getClientRegistrationId()).principal(request.getPrincipal())
                    .attributes(attributes -> { attributes.putAll(request.getAttributes()); attributes.put(ServerWebExchange.class.getName(), exchange); }).build();
            return Mono.usingWhen(acquire(lockName), lease -> delegate.authorize(nativeRequest)
                            .timeout(properties.getRefresh().getTokenTimeout())
                            .switchIfEmpty(Mono.error(unavailable("Edge resource authorization is required")))
                            .onErrorResume(failure -> {
                                if (failure instanceof OAuth2AuthorizationException known && "invalid_grant".equals(known.getError().getErrorCode())) { return Mono.error(failure); }
                                return clients.closeUnknown(request.getPrincipal(), exchange, lifecycle)
                                        .then(Mono.error(unavailable("Edge token refresh outcome is unknown")));
                            }),
                    LockLease::release,
                    (lease, failure) -> lease.release(),
                    LockLease::release);
        }), lifecycle -> clients.closeUnknown(request.getPrincipal(), exchange, lifecycle));
    }

    private Mono<LockLease> acquire(String lockName) {
        return Mono.defer(() -> {
            long threadId = Thread.currentThread().getId();
            RLockReactive lock = redisson.getLock(lockName);
            return lock.tryLock(
                            properties.getRefresh().getLockWait().toMillis(),
                            properties.getRefresh().getLockLease().toMillis(),
                            TimeUnit.MILLISECONDS,
                            threadId)
                    .flatMap(acquired -> acquired
                            ? Mono.just(new LockLease(lock, threadId))
                            : Mono.error(unavailable("Edge token refresh lock is unavailable")));
        });
    }

    private Mono<ServerWebExchange> exchange(OAuth2AuthorizeRequest request) {
        ServerWebExchange explicit = request.getAttribute(ServerWebExchange.class.getName());
        if (explicit != null) {
            return Mono.just(explicit);
        }
        return Mono.deferContextual(context -> ServerWebExchangeContextFilter.getExchange(context)
                .map(Mono::just)
                .orElseGet(() -> Mono.error(unavailable("Edge request context is unavailable"))));
    }

    private static OAuth2AuthorizationException unavailable(String message) {
        return new OAuth2AuthorizationException(new OAuth2Error(TEMPORARILY_UNAVAILABLE), message);
    }

    private record LockLease(RLockReactive lock, long threadId) {
        private Mono<Void> release() {
            return lock.unlock(threadId);
        }
    }
}
