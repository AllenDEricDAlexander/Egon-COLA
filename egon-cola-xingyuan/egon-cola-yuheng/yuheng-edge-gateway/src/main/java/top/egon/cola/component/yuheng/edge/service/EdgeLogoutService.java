package top.egon.cola.component.yuheng.edge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutIntentStore.PendingIntent;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeAuthorizedClientRepository;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeOAuthTokenRevocationClient;

import java.time.Clock;
import java.time.Instant;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service("edgeLogoutService")
@RequiredArgsConstructor
public class EdgeLogoutService {
    public enum RemoteState {
        CONFIRMED,
        PENDING,
        UNKNOWN
    }

    private static final String BFF_COOKIE = "__Host-egon_bff";

    @Qualifier("edgeAuthorizedClientRepository")
    private final EdgeAuthorizedClientRepository authorizedClients;
    @Qualifier("edgeLogoutIntentStore")
    private final EdgeLogoutIntentStore intentStore;
    @Qualifier("edgeOAuthTokenRevocationClient")
    private final EdgeOAuthTokenRevocationClient revocationClient;
    @Qualifier("edgeClock")
    private final Clock clock;

    public Mono<RemoteState> logout(Authentication principal, ServerWebExchange exchange) {
        if (!(principal instanceof OAuth2AuthenticationToken) || !principal.isAuthenticated()) {
            return Mono.error(new IllegalArgumentException("Authenticated BFF session is required for logout"));
        }
        var bindings = authorizedClients.bindingComponent();
        return bindings.withLifecycle(exchange, lease -> authorizedClients.loadAll(principal, exchange)
                .flatMap(clients -> persistAndClose(clients, principal, exchange, bindings, lease)))
                .onErrorResume(failure -> {
                    log.warn("BFF lifecycle could not be closed; failureType={}", failure.getClass().getSimpleName());
                    return invalidateLocalSession(exchange).thenReturn(new CompletedLocalLogoutVO(List.of(), List.of(), RemoteState.UNKNOWN));
                })
                .flatMap(local -> local.state() != null ? Mono.just(local.state()) : revokeAndAcknowledge(local.intents(), local.messageIds()));
    }

    public Mono<Void> process(PendingIntent pending) {
        return revocationClient.revoke(pending.intent())
                .then(intentStore.acknowledge(pending.messageId()))
                .doOnSuccess(ignored -> log.info("BFF remote revocation confirmed; intentId={}",
                        pending.intent().intentId()));
    }

    private Mono<CompletedLocalLogoutVO> persistAndClose(List<OAuth2AuthorizedClient> clients, Authentication principal,
            ServerWebExchange exchange, top.egon.cola.component.yuheng.edge.service.impl.EdgeSessionBindingComponent bindings,
            top.egon.cola.component.yuheng.edge.service.impl.EdgeSessionBindingComponent.LifecycleLease lease) {
        if (clients.stream().anyMatch(client -> client.getRefreshToken() == null)) {
            return bindings.beginClosing(lease).then(clearLocally(principal, exchange)).then(bindings.finishClosing(lease))
                    .thenReturn(new CompletedLocalLogoutVO(List.of(), List.of(), RemoteState.UNKNOWN));
        }
        List<EdgeLogoutRevocationIntent> intents = clients.stream().map(this::intent).toList();
        Mono<CompletedLocalLogoutVO> persisted = intents.isEmpty()
                ? Mono.just(new CompletedLocalLogoutVO(List.of(), List.of(), RemoteState.CONFIRMED))
                : intentStore.enqueueAll(intents).map(ids -> new CompletedLocalLogoutVO(intents, ids,
                        ids.size() == intents.size() ? null : RemoteState.UNKNOWN));
        return persisted.onErrorResume(failure -> {
                    log.error("BFF revocation intent batch could not be persisted; failureType={}", failure.getClass().getSimpleName());
                    return Mono.just(new CompletedLocalLogoutVO(List.of(), List.of(), RemoteState.UNKNOWN));
                })
                .flatMap(local -> bindings.beginClosing(lease).then(clearLocally(principal, exchange)).then(bindings.finishClosing(lease)).thenReturn(local));
    }

    private record CompletedLocalLogoutVO(List<EdgeLogoutRevocationIntent> intents, List<String> messageIds, RemoteState state) {
        private CompletedLocalLogoutVO { intents = List.copyOf(intents); messageIds = List.copyOf(messageIds); }
        @Override public String toString() { return "CompletedLocalLogoutVO[state=" + state + ", familyCount=" + intents.size() + "]"; }
    }

    private Mono<RemoteState> revokeAndAcknowledge(
            List<EdgeLogoutRevocationIntent> intents,
            List<String> messageIds) {
        if (messageIds.size() != intents.size()) {
            return Mono.just(RemoteState.PENDING);
        }
        return Flux.range(0, intents.size())
                .concatMap(index -> revocationClient.revoke(intents.get(index))
                        .then(intentStore.acknowledge(messageIds.get(index)))
                        .thenReturn(true)
                        .onErrorResume(exception -> {
                            log.warn("BFF remote revocation remains pending; intentId={}",
                                    intents.get(index).intentId());
                            return Mono.just(false);
                        }))
                .all(Boolean::booleanValue)
                .map(confirmed -> confirmed ? RemoteState.CONFIRMED : RemoteState.PENDING);
    }

    private EdgeLogoutRevocationIntent intent(OAuth2AuthorizedClient client) {
        String issuer = issuer(client);
        return new EdgeLogoutRevocationIntent(
                UUID.randomUUID().toString(),
                client.getClientRegistration().getRegistrationId(),
                issuer,
                tenantId(issuer),
                client.getClientRegistration().getClientId(),
                client.getRefreshToken().getTokenValue(),
                "refresh_token",
                Instant.now(clock));
    }

    private Mono<Void> removeAll(Authentication principal, ServerWebExchange exchange) {
        return authorizedClients.removeAll(principal, exchange);
    }

    private Mono<Void> clearLocally(Authentication principal, ServerWebExchange exchange) {
        return removeAll(principal, exchange)
                .doOnError(exception -> log.warn("BFF terminal AuthorizedClient cleanup failed; failureType={}", exception.getClass().getSimpleName()))
                .then(invalidateLocalSession(exchange));
    }

    private Mono<Void> invalidateLocalSession(ServerWebExchange exchange) {
        ResponseCookie expired = ResponseCookie.from(BFF_COOKIE, "")
                .path("/")
                .secure(true)
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        exchange.getResponse().addCookie(expired);
        return exchange.getSession().flatMap(session -> session.invalidate());
    }

    private static String tenantId(String issuer) {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("Edge issuer is unavailable for logout");
        }
        String path = URI.create(issuer).getPath();
        if ("/platform".equals(path)) {
            return "PLATFORM";
        }
        if (path != null && path.matches("/tenants/[1-9][0-9]*")) {
            return path.substring("/tenants/".length());
        }
        throw new IllegalStateException("Edge logout issuer tenant path is invalid");
    }

    private static String issuer(OAuth2AuthorizedClient client) {
        String issuer = client.getClientRegistration().getProviderDetails().getIssuerUri();
        if (issuer == null || issuer.isBlank()) {
            Object metadataIssuer = client.getClientRegistration().getProviderDetails()
                    .getConfigurationMetadata().get("issuer");
            issuer = metadataIssuer instanceof String value ? value : null;
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("Edge issuer is unavailable for logout");
        }
        return URI.create(issuer).toASCIIString();
    }
}
