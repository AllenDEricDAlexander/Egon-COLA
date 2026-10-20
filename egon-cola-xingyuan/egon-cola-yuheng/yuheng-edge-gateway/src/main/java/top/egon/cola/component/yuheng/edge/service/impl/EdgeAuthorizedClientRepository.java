package top.egon.cola.component.yuheng.edge.service.impl;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutIntentStore;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutRevocationIntent;
import top.egon.cola.component.yuheng.edge.service.domain.bo.EdgeSessionBindingBO;
import top.egon.cola.component.yuheng.edge.service.domain.enums.EdgeSessionBindingStateEnum;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;

/** Stores only fully verified, exactly registered clients under the authoritative session fence. */
@Component("edgeAuthorizedClientRepository")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class EdgeAuthorizedClientRepository implements ServerOAuth2AuthorizedClientRepository {
    public static final String SESSION_BINDING_ATTRIBUTE = EdgeSessionBindingComponent.BINDING_ID;
    public static final String PENDING_REQUEST = EdgeAuthorizedClientRepository.class.getName() + ".pendingRequest";
    private static final String SAVED_VERIFICATION = EdgeAuthorizedClientRepository.class.getName() + ".savedVerification";
    private static final String PURPOSE = "authorized-client-v2";

    @Qualifier("redisson") private final RedissonClient redisson;
    @Qualifier("reactiveClientRegistrationRepository") private final ReactiveClientRegistrationRepository registrations;
    @Qualifier("edgeOAuthProperties") private final EdgeOAuthProperties properties;
    @Qualifier("edgeTokenCipher") private final EdgeTokenCipher cipher;
    @Qualifier("edgeClock") private final Clock clock;
    @Qualifier("edgeSessionBindingComponent") private final EdgeSessionBindingComponent bindings;
    @Qualifier("edgeLogoutIntentStore") private final EdgeLogoutIntentStore intents;
    private final JdkSerializationRedisSerializer serializer = new JdkSerializationRedisSerializer();

    /** Source compatibility only: a former host must provide the authoritative adapter before use. */
    public EdgeAuthorizedClientRepository(RedissonClient redisson, ReactiveClientRegistrationRepository registrations,
            EdgeOAuthProperties properties, EdgeTokenCipher cipher, Clock clock) {
        this(redisson, registrations, properties, cipher, clock, null, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> Mono<T> loadAuthorizedClient(String registrationId, Authentication principal, ServerWebExchange exchange) {
        if (!authenticated(principal)) { return Mono.empty(); }
        if (bindings == null || intents == null) { return configured().then(Mono.empty()); }
        return configured().then(bindings.load(exchange)).flatMap(binding -> {
            requireActive(binding, principal);
            return requireRegistration(registrationId, binding).flatMap(registration -> readOne(binding, registration)).map(client -> (T) client);
        });
    }

    @Override
    public Mono<Void> saveAuthorizedClient(OAuth2AuthorizedClient client, Authentication principal, ServerWebExchange exchange) {
        if (client == null || !authenticated(principal) || !principal.getName().equals(client.getPrincipalName())
                || client.getAccessToken() == null || client.getRefreshToken() == null) {
            return Mono.error(EdgeSessionBindingComponent.invalid("Edge native authorized client is incomplete"));
        }
        if (bindings == null || intents == null) { return configured(); }
        return Mono.defer(() -> {
            var attempted = new java.util.concurrent.atomic.AtomicBoolean();
            Mono<Void> persist = configured().then(bindings.withLifecycle(exchange, lease -> Mono.usingWhen(Mono.just(client), ignored -> bindings.state(lease).flatMap(binding -> {
            String id = client.getClientRegistration().getRegistrationId();
            String alias = bindings.aliasForRegistration(id);
            return requireRegistration(id, binding).flatMap(registration -> {
                if (!sameRegistration(registration, client.getClientRegistration())) {
                    return Mono.error(EdgeSessionBindingComponent.invalid("Edge authorized client registration has changed"));
                }
                validateTransaction(exchange, lease, alias, registration);
                return bindings.verifyClient(client, principal, UUID.randomUUID().toString().replace("-", "")).flatMap(result -> {
                    if (!(result instanceof AccessTokenVerification.Valid<IdentityPrincipal> valid) || valid.proof() == null) {
                        return Mono.error(result instanceof AccessTokenVerification.Unavailable<?>
                                ? EdgeSessionBindingComponent.unavailable("Edge token verification dependency is unavailable")
                                : EdgeSessionBindingComponent.invalid("Edge resource access token is invalid"));
                    }
                    return bindings.canonical(lease, valid.principal(), valid.proof()).flatMap(desired -> {
                        String field = attributeKey(alias, registration, desired);
                        return Mono.fromCallable(() -> new String(cipher.encrypt(serializer.serialize(client), PURPOSE, context(desired, alias, registration, field)), StandardCharsets.US_ASCII))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(encrypted -> bindings.writeClient(lease, desired, field, encrypted))
                                .then(Mono.<Void>fromRunnable(() -> exchange.getAttributes().put(SAVED_VERIFICATION, new VerifiedClientVO(client, valid, desired.getGeneration()))));
                    });
                });
            });
        }), ignored -> Mono.empty(),
                (ignored, failure) -> compensateOnce(client, exchange, attempted),
                ignored -> compensateOnce(client, exchange, attempted))))
                .onErrorResume(failure -> compensateOnce(client, exchange, attempted).then(Mono.error(failure)));
            return persist.onErrorMap(RuntimeException.class, failure -> {
                    Throwable cause = failure;
                    for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
                        if (cause instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException unavailable
                                && "temporarily_unavailable".equals(unavailable.getError().getErrorCode())) {
                            return EdgeSessionBindingComponent.unavailable("Edge token family persistence is unavailable");
                        }
                        if (cause.getCause() == cause) { break; }
                    }
                    return failure;
                });
        });
    }

    private Mono<Void> compensateOnce(OAuth2AuthorizedClient client, ServerWebExchange exchange, java.util.concurrent.atomic.AtomicBoolean attempted) {
        return Mono.defer(() -> attempted.compareAndSet(false, true) ? compensate(client)
                .onErrorResume(handoffFailure -> terminateFailedCompensation(exchange, handoffFailure)) : Mono.empty());
    }

    private Mono<Void> terminateFailedCompensation(ServerWebExchange exchange, Throwable failure) {
        log.error("Edge token family compensation is not confirmed; failureType={}", failure.getClass().getSimpleName());
        var lease = exchange.<EdgeSessionBindingComponent.LifecycleLease>getAttribute(EdgeSessionBindingComponent.LEASE);
        if (lease == null) {
            return bindings.withLifecycle(exchange, current -> bindings.close(current).then(bindings.clearClients(current)))
                    .onErrorResume(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class,
                            closed -> "invalid_token".equals(closed.getError().getErrorCode()) ? Mono.empty() : Mono.error(closed))
                    .then(exchange.getSession().flatMap(session -> session.invalidate()))
                    .then(Mono.error(EdgeSessionBindingComponent.unavailable("Edge family compensation outcome is unknown")));
        }
        return bindings.close(lease).then(bindings.clearClients(lease)).onErrorResume(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class,
                closed -> "invalid_token".equals(closed.getError().getErrorCode()) ? Mono.empty() : Mono.error(closed))
                .then(exchange.getSession().flatMap(session -> session.invalidate()))
                .then(Mono.error(EdgeSessionBindingComponent.unavailable("Edge family compensation outcome is unknown")));
    }

    public Mono<AccessTokenVerification<IdentityPrincipal>> verifyForForward(OAuth2AuthorizedClient client, Authentication principal, ServerWebExchange exchange) {
        Object saved = exchange.getAttributes().remove(SAVED_VERIFICATION);
        if (saved instanceof VerifiedClientVO checked && checked.client().getAccessToken().getTokenValue().equals(client.getAccessToken().getTokenValue())
                && checked.client().getClientRegistration().getRegistrationId().equals(client.getClientRegistration().getRegistrationId())) {
            return bindings.load(exchange).map(binding -> binding.getState() == EdgeSessionBindingStateEnum.ACTIVE
                    && binding.getGeneration() == checked.generation() && binding.getSubject().equals(principal.getName())
                    ? checked.verification() : new AccessTokenVerification.Invalid<IdentityPrincipal>("JWT_SESSION_INVALID"));
        }
        return bindings.verifyClient(client, principal, UUID.randomUUID().toString().replace("-", ""));
    }

    public void clearRequestVerification(ServerWebExchange exchange) { exchange.getAttributes().remove(SAVED_VERIFICATION); }

    private record VerifiedClientVO(OAuth2AuthorizedClient client, AccessTokenVerification.Valid<IdentityPrincipal> verification, long generation) {
        @Override public String toString() { return "VerifiedClientVO[generation=" + generation + "]"; }
    }

    @Override
    public Mono<Void> removeAuthorizedClient(String registrationId, Authentication principal, ServerWebExchange exchange) {
        if (!authenticated(principal)) { return Mono.empty(); }
        if (bindings == null || intents == null) { return configured(); }
        return configured().then(bindings.withLifecycle(exchange, lease -> bindings.state(lease).flatMap(binding -> {
            requireActive(binding, principal);
            return requireRegistration(registrationId, binding).flatMap(registration -> {
                String alias = bindings.aliasForRegistration(registrationId);
                String field = attributeKey(alias, registration, binding);
                return bindings.removeClient(lease, field);
            });
        })));
    }

    public EdgeSessionBindingComponent bindingComponent() {
        if (bindings == null || intents == null) { throw EdgeSessionBindingComponent.unavailable("Edge authoritative client adapter is required"); }
        return bindings;
    }

    public Mono<String> sessionBindingId(ServerWebExchange exchange) {
        if (bindings == null || intents == null) { return configured().then(Mono.empty()); }
        return configured().then(bindings.load(exchange)).map(EdgeSessionBindingBO::getBindingId);
    }

    public Mono<List<OAuth2AuthorizedClient>> loadAll(Authentication principal, ServerWebExchange exchange) {
        if (!authenticated(principal)) { return Mono.just(List.of()); }
        if (bindings == null || intents == null) { return configured().then(Mono.empty()); }
        return configured().then(bindings.load(exchange)).flatMap(binding -> {
            requireActive(binding, principal);
            return Mono.fromCallable(() -> map(binding.getBindingId()).readAllMap()).subscribeOn(Schedulers.boundedElastic())
                    .flatMap(stored -> {
                        if (stored.size() > properties.getSession().getMaxAuthorizedClients()) {
                            return Mono.error(EdgeSessionBindingComponent.unavailable("Edge authorized client catalog exceeds its bound"));
                        }
                        return Flux.fromIterable(properties.getResources().keySet().stream()
                                        .filter(alias -> properties.getResources().get(alias).getIssuerRegistrationIds().containsKey(binding.getIssuer().toASCIIString())).sorted().toList())
                                .concatMap(alias -> requireRegistration(bindings.resolveRegistration(alias, binding.getIssuer()), binding)
                                        .flatMap(registration -> readOne(binding, registration)))
                                .sort(java.util.Comparator.comparing(client -> client.getClientRegistration().getRegistrationId()))
                                .collectList().flatMap(clients -> clients.size() == stored.size() ? Mono.just(List.copyOf(clients))
                                        : Mono.error(EdgeSessionBindingComponent.unavailable("Edge stored client tuple is no longer configured")));
                    });
        });
    }

    public Mono<Void> removeAll(Authentication principal, ServerWebExchange exchange) {
        if (bindings == null || intents == null) { return configured(); }
        var lease = exchange.<EdgeSessionBindingComponent.LifecycleLease>getAttribute(EdgeSessionBindingComponent.LEASE);
        if (lease == null) { return Mono.error(EdgeSessionBindingComponent.unavailable("Edge terminal cleanup requires the lifecycle owner")); }
        return bindings.clearClients(lease);
    }

    /** An unknown consumed-RT outcome requires explicit reauthorization, with durable family cleanup. */
    public Mono<Void> closeUnknown(Authentication principal, ServerWebExchange exchange, EdgeSessionBindingComponent.LifecycleLease lease) {
        return loadAll(principal, exchange).flatMapMany(Flux::fromIterable).concatMap(this::compensate).then()
                .onErrorResume(failure -> {
                    log.warn("Edge unknown refresh cleanup is not confirmed; failureType={}", failure.getClass().getSimpleName());
                    return Mono.empty();
                }).then(bindings.close(lease)).then(bindings.clearClients(lease))
                .then(exchange.getSession().flatMap(session -> session.invalidate()));
    }

    private Mono<OAuth2AuthorizedClient> readOne(EdgeSessionBindingBO binding, ClientRegistration registration) {
        String alias = bindings.aliasForRegistration(registration.getRegistrationId());
        String field = attributeKey(alias, registration, binding);
        return Mono.fromCallable(() -> {
            String encrypted = map(binding.getBindingId()).get(field);
            if (encrypted == null) { return null; }
            Object decoded = serializer.deserialize(cipher.decrypt(encrypted.getBytes(StandardCharsets.US_ASCII), PURPOSE, context(binding, alias, registration, field)));
            if (!(decoded instanceof OAuth2AuthorizedClient client) || !binding.getSubject().equals(client.getPrincipalName())
                    || !sameRegistration(registration, client.getClientRegistration()) || client.getRefreshToken() == null) {
                throw EdgeSessionBindingComponent.unavailable("Edge stored client does not match the canonical tuple");
            }
            return client;
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(failure -> failure instanceof IllegalArgumentException
                                || failure instanceof org.springframework.data.redis.serializer.SerializationException
                                || failure instanceof org.redisson.client.RedisException,
                        failure -> new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                                new org.springframework.security.oauth2.core.OAuth2Error("temporarily_unavailable"),
                                "Edge encrypted client state is unavailable", failure));
    }

    private Mono<ClientRegistration> requireRegistration(String id, EdgeSessionBindingBO binding) {
        String alias = bindings.aliasForRegistration(id);
        var resource = bindings.resource(alias);
        return registrations.findByRegistrationId(id).switchIfEmpty(Mono.error(EdgeSessionBindingComponent.unavailable("Edge registration is unavailable")))
                .map(registration -> {
                    String issuer = registration.getProviderDetails().getIssuerUri();
                    if (!id.equals(resource.getIssuerRegistrationIds().get(issuer)) || !resource.getScopes().equals(registration.getScopes())
                            || binding.getIssuer() != null && !binding.getIssuer().toASCIIString().equals(issuer)) {
                        throw EdgeSessionBindingComponent.invalid("Edge held registration disagrees with the resource issuer tuple");
                    }
                    return registration;
                });
    }

    private void validateTransaction(ServerWebExchange exchange, EdgeSessionBindingComponent.LifecycleLease lease, String alias, ClientRegistration registration) {
        Object pending = exchange.getAttribute(PENDING_REQUEST);
        if (pending instanceof OAuth2AuthorizationRequest request) {
            if (!lease.bindingId().equals(request.getAttribute(EdgeSessionBindingComponent.BINDING_ID))
                    || !(request.getAttribute(EdgeSessionBindingComponent.GENERATION) instanceof Number generation) || generation.longValue() != lease.generation()
                    || !alias.equals(request.getAttribute(EdgeSessionBindingComponent.RESOURCE_ALIAS))
                    || !registration.getRegistrationId().equals(request.getAttribute(EdgeSessionBindingComponent.REGISTRATION_ID))
                    || !registration.getProviderDetails().getIssuerUri().equals(request.getAttribute(EdgeSessionBindingComponent.ISSUER))
                    || !bindings.resource(alias).getResourceUri().toASCIIString().equals(request.getAttribute(EdgeSessionBindingComponent.RESOURCE))
                    || !exchange.getRequest().getPath().value().equals("/login/oauth2/code/" + registration.getRegistrationId())) {
                throw EdgeSessionBindingComponent.invalid("Edge callback transaction does not match its pending tuple");
            }
        } else if (exchange.getAttribute(EdgeSessionBindingComponent.LEASE) != lease
                || !registration.getRegistrationId().equals(exchange.getAttribute(EdgeSessionBindingComponent.REGISTRATION_ID))) {
            throw EdgeSessionBindingComponent.invalid("Edge native refresh has no server-owned resource selection");
        }
    }

    private Mono<Void> compensate(OAuth2AuthorizedClient client) {
        if (intents == null || bindings == null) { return Mono.error(EdgeSessionBindingComponent.unavailable("Edge authoritative client adapter is required")); }
        return registrations.findByRegistrationId(client.getClientRegistration().getRegistrationId())
                .filter(registration -> sameRegistration(registration, client.getClientRegistration()))
                .switchIfEmpty(Mono.error(EdgeSessionBindingComponent.unavailable("Edge compensation registration is unavailable")))
                .flatMap(registration -> {
                    bindings.aliasForRegistration(registration.getRegistrationId());
                    String issuer = registration.getProviderDetails().getIssuerUri();
                    var intent = new EdgeLogoutRevocationIntent(UUID.randomUUID().toString(), registration.getRegistrationId(), issuer,
                            URI.create(issuer).getPath().substring("/tenants/".length()), registration.getClientId(), client.getRefreshToken().getTokenValue(), "refresh_token", clock.instant());
                    return intents.enqueue(intent).then();
                });
    }

    private Mono<Void> configured() {
        return bindings == null || intents == null ? Mono.error(EdgeSessionBindingComponent.unavailable("Edge authoritative client adapter is required")) : Mono.empty();
    }

    private void requireActive(EdgeSessionBindingBO binding, Authentication principal) {
        if (binding.getState() != EdgeSessionBindingStateEnum.ACTIVE || !binding.getSubject().equals(principal.getName())) {
            throw EdgeSessionBindingComponent.invalid("Edge canonical session identity is not active");
        }
    }

    private boolean sameRegistration(ClientRegistration first, ClientRegistration second) {
        return first.getRegistrationId().equals(second.getRegistrationId()) && first.getClientId().equals(second.getClientId())
                && first.getProviderDetails().getIssuerUri().equals(second.getProviderDetails().getIssuerUri())
                && first.getScopes().equals(second.getScopes()) && first.getRedirectUri().equals(second.getRedirectUri())
                && first.getClientAuthenticationMethod().equals(second.getClientAuthenticationMethod())
                && first.getProviderDetails().getAuthorizationUri().equals(second.getProviderDetails().getAuthorizationUri())
                && first.getProviderDetails().getTokenUri().equals(second.getProviderDetails().getTokenUri())
                && first.getProviderDetails().getJwkSetUri().equals(second.getProviderDetails().getJwkSetUri());
    }

    private static boolean authenticated(Authentication principal) {
        return principal != null && principal.isAuthenticated() && !(principal instanceof AnonymousAuthenticationToken)
                && principal.getName() != null && !principal.getName().isBlank();
    }

    private String attributeKey(String alias, ClientRegistration registration, EdgeSessionBindingBO binding) {
        return EdgeSessionBindingComponent.digest(alias, registration.getRegistrationId(), binding.getIssuer().toASCIIString(), binding.getTenantId(),
                registration.getClientId(), bindings.resource(alias).getResourceUri().toASCIIString(), binding.getSubject());
    }

    private String context(EdgeSessionBindingBO binding, String alias, ClientRegistration registration, String field) {
        return String.join(":", binding.getBindingId(), alias, registration.getRegistrationId(), binding.getIssuer().toASCIIString(), field, "2");
    }

    private RMapCache<String, String> map(String id) { return redisson.getMapCache(bindings.authorizedKey(id), StringCodec.INSTANCE); }
}
