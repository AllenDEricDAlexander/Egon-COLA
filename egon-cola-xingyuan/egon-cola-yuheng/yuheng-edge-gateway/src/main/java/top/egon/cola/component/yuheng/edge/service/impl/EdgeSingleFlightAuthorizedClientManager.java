package top.egon.cola.component.yuheng.edge.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.security.core.Authentication;
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

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@Slf4j
public class EdgeSingleFlightAuthorizedClientManager implements ReactiveOAuth2AuthorizedClientManager {
    private static final String LOCK_PREFIX = "egon:bff:oauth-refresh:v1:";
    private static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";

    private final ReactiveOAuth2AuthorizedClientManager delegate;
    private final ReactiveClientRegistrationRepository registrations;
    private final RedissonReactiveClient redisson;
    private final EdgeOAuthProperties properties;

    @Override
    public Mono<org.springframework.security.oauth2.client.OAuth2AuthorizedClient> authorize(
            OAuth2AuthorizeRequest authorizeRequest) {
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
            OAuth2AuthorizeRequest request,
            ServerWebExchange exchange,
            ClientRegistration registration) {
        Authentication principal = request.getPrincipal();
        return exchange.getSession().flatMap(session -> {
            Object binding = session.getAttribute(EdgeAuthorizedClientRepository.SESSION_BINDING_ATTRIBUTE);
            if (!(binding instanceof String bindingId) || bindingId.isBlank()) {
                return Mono.error(unavailable("Edge session binding is unavailable"));
            }
            String lockName = LOCK_PREFIX + digest(
                    bindingId,
                    issuer(registration),
                    tenantId(registration),
                    registration.getClientId(),
                    registration.getRegistrationId(),
                    principal.getName());
            return Mono.usingWhen(
                    acquire(lockName),
                    lease -> delegate.authorize(request)
                            .timeout(properties.getRefresh().getTokenTimeout())
                            .onErrorMap(TimeoutException.class,
                                    exception -> unavailable("Edge token refresh timed out")),
                    LockLease::release,
                    (lease, exception) -> lease.release(),
                    LockLease::release);
        });
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

    private static String issuer(ClientRegistration registration) {
        String value = registration.getProviderDetails().getIssuerUri();
        if (value == null) {
            Object metadataIssuer = registration.getProviderDetails().getConfigurationMetadata().get("issuer");
            value = metadataIssuer instanceof String issuer ? issuer : null;
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Edge refresh issuer is unavailable");
        }
        return URI.create(value).normalize().toString().replaceAll("/$", "");
    }

    private static String tenantId(ClientRegistration registration) {
        String path = URI.create(issuer(registration)).getPath();
        if ("/platform".equals(path)) {
            return "PLATFORM";
        }
        if (path != null && path.matches("/tenants/[1-9][0-9]*")) {
            return path.substring("/tenants/".length());
        }
        throw new IllegalArgumentException("Edge refresh issuer tenant path is invalid");
    }

    private static String digest(String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                byte[] encoded = field.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(encoded.length).array());
                digest.update(encoded);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
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
