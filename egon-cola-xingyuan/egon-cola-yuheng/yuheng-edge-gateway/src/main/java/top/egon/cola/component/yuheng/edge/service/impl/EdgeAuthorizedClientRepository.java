package top.egon.cola.component.yuheng.edge.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component("edgeAuthorizedClientRepository")
@RequiredArgsConstructor
@Slf4j
public class EdgeAuthorizedClientRepository implements ServerOAuth2AuthorizedClientRepository {
    public static final String SESSION_BINDING_ATTRIBUTE =
            EdgeAuthorizedClientRepository.class.getName() + ".SESSION_BINDING_ID";
    private static final String MAP_PREFIX = "egon:bff:authorized-client:v1:";
    private static final String PURPOSE = "authorized-client";

    @Qualifier("redisson")
    private final RedissonClient redisson;
    @Qualifier("clientRegistrationRepository")
    private final ReactiveClientRegistrationRepository registrations;
    @Qualifier("edgeOAuthProperties")
    private final EdgeOAuthProperties properties;
    @Qualifier("edgeTokenCipher")
    private final EdgeTokenCipher cipher;
    @Qualifier("edgeClock")
    private final Clock clock;
    private final JdkSerializationRedisSerializer serializer = new JdkSerializationRedisSerializer();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> Mono<T> loadAuthorizedClient(
            String clientRegistrationId,
            Authentication principal,
            ServerWebExchange exchange) {
        if (!authenticated(principal)) {
            return Mono.empty();
        }
        return requireRegistration(clientRegistrationId)
                .flatMap(registration -> exchange.getSession()
                        .flatMap(session -> loadOne(session, registration, principal))
                        .map(client -> (T) client));
    }

    @Override
    public Mono<Void> saveAuthorizedClient(
            OAuth2AuthorizedClient authorizedClient,
            Authentication principal,
            ServerWebExchange exchange) {
        if (authorizedClient == null || !authenticated(principal)
                || !principal.getName().equals(authorizedClient.getPrincipalName())) {
            return Mono.error(new IllegalArgumentException("Edge AuthorizedClient principal is invalid"));
        }
        return requireRegistration(authorizedClient.getClientRegistration().getRegistrationId())
                .flatMap(registration -> {
                    if (!sameRegistration(registration, authorizedClient.getClientRegistration())
                            || authorizedClient.getRefreshToken() == null) {
                        return Mono.error(new IllegalArgumentException(
                                "Edge BFF requires the exact confidential offline_access registration"));
                    }
                    return exchange.getSession().flatMap(session -> {
                        if (expired(session)) {
                            return Mono.error(new IllegalStateException("Edge BFF session has expired"));
                        }
                        String bindingId = bindingId(session, true);
                        String field = attributeKey(registration, principal);
                        String context = context(bindingId, field);
                        byte[] serialized = serializer.serialize(authorizedClient);
                        String encrypted = new String(
                                cipher.encrypt(serialized, PURPOSE, context),
                                StandardCharsets.US_ASCII);
                        return Mono.fromCallable(() -> {
                                    RMapCache<String, String> authorized = map(bindingId);
                                    if (!authorized.containsKey(field)
                                            && authorized.size() >= properties.getSession().getMaxAuthorizedClients()) {
                                        throw new IllegalStateException("Edge session AuthorizedClient limit reached");
                                    }
                                    return authorized.fastPut(
                                            field,
                                            encrypted,
                                            absoluteRemaining(session).toSeconds(),
                                            TimeUnit.SECONDS,
                                            properties.getSession().getIdleTimeout().toSeconds(),
                                            TimeUnit.SECONDS);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .then();
                    });
                });
    }

    @Override
    public Mono<Void> removeAuthorizedClient(
            String clientRegistrationId,
            Authentication principal,
            ServerWebExchange exchange) {
        if (!authenticated(principal)) {
            return Mono.empty();
        }
        return registrations.findByRegistrationId(clientRegistrationId)
                .flatMap(registration -> exchange.getSession().flatMap(session -> {
                    String bindingId = bindingId(session, false);
                    if (bindingId == null) {
                        return Mono.empty();
                    }
                    String field = attributeKey(registration, principal);
                    return Mono.fromRunnable(() -> remove(bindingId, field))
                            .subscribeOn(Schedulers.boundedElastic());
                }))
                .then();
    }

    public Mono<String> sessionBindingId(ServerWebExchange exchange) {
        return exchange.getSession()
                .map(session -> bindingId(session, false))
                .flatMap(Mono::justOrEmpty);
    }

    public Mono<List<OAuth2AuthorizedClient>> loadAll(
            Authentication principal,
            ServerWebExchange exchange) {
        return exchange.getSession().flatMap(session -> {
            String bindingId = bindingId(session, false);
            if (bindingId == null) {
                return Mono.just(List.of());
            }
            if (expired(session)) {
                return Mono.fromRunnable(() -> map(bindingId).delete())
                        .subscribeOn(Schedulers.boundedElastic())
                        .then(session.invalidate())
                        .thenReturn(List.of());
            }
            return Mono.fromCallable(() -> loadAllFromMap(bindingId))
                    .subscribeOn(Schedulers.boundedElastic());
        });
    }

    public Mono<Void> removeAll(Authentication principal, ServerWebExchange exchange) {
        return exchange.getSession().flatMap(session -> {
            String bindingId = bindingId(session, false);
            if (bindingId == null) {
                return Mono.empty();
            }
            return Mono.fromRunnable(() -> map(bindingId).delete())
                    .subscribeOn(Schedulers.boundedElastic())
                    .doFinally(signal -> session.getAttributes().remove(SESSION_BINDING_ATTRIBUTE));
        }).then();
    }

    private Mono<OAuth2AuthorizedClient> loadOne(
            WebSession session,
            ClientRegistration registration,
            Authentication principal) {
        String bindingId = bindingId(session, false);
        if (bindingId == null) {
            return Mono.empty();
        }
        if (expired(session)) {
            return Mono.fromRunnable(() -> map(bindingId).delete())
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(session.invalidate())
                    .then(Mono.empty());
        }
        String field = attributeKey(registration, principal);
        return Mono.fromCallable(() -> read(bindingId, field))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(Mono::justOrEmpty)
                .filter(client -> client.getPrincipalName().equals(principal.getName()));
    }

    private Mono<ClientRegistration> requireRegistration(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Edge registration id is required"));
        }
        return registrations.findByRegistrationId(registrationId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Edge registration is unknown")))
                .map(this::validateRegistration);
    }

    private ClientRegistration validateRegistration(ClientRegistration registration) {
        String issuer = issuer(registration);
        URI issuerUri = URI.create(issuer);
        URI configured = properties.getUpstreams().getAuthorizationServer();
        if (!sameOrigin(configured, issuerUri)
                || !"https".equalsIgnoreCase(issuerUri.getScheme())
                || registration.getAuthorizationGrantType() != AuthorizationGrantType.AUTHORIZATION_CODE
                || registration.getClientAuthenticationMethod() != ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                || registration.getClientSecret() == null
                || registration.getClientSecret().isBlank()
                || !registration.getScopes().contains("offline_access")) {
            throw new IllegalArgumentException("Edge BFF requires a trusted confidential offline_access registration");
        }
        tenantId(issuer);
        return registration;
    }

    private static boolean sameRegistration(ClientRegistration expected, ClientRegistration actual) {
        return expected.getRegistrationId().equals(actual.getRegistrationId())
                && expected.getClientId().equals(actual.getClientId())
                && issuer(expected).equals(issuer(actual));
    }

    private static boolean authenticated(Authentication principal) {
        return principal != null
                && principal.isAuthenticated()
                && !(principal instanceof AnonymousAuthenticationToken)
                && principal.getName() != null
                && !principal.getName().isBlank();
    }

    private OAuth2AuthorizedClient read(String bindingId, String field) {
        String encrypted = map(bindingId).get(field);
        if (encrypted == null) {
            return null;
        }
        try {
            byte[] serialized = cipher.decrypt(
                    encrypted.getBytes(StandardCharsets.US_ASCII), PURPOSE, context(bindingId, field));
            Object decoded = serializer.deserialize(serialized);
            if (!(decoded instanceof OAuth2AuthorizedClient client)) {
                throw new IllegalStateException("Edge AuthorizedClient entry has the wrong type");
            }
            return client;
        } catch (RuntimeException exception) {
            log.warn("Edge AuthorizedClient entry could not be authenticated; failureType={}",
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private List<OAuth2AuthorizedClient> loadAllFromMap(String bindingId) {
        Map<String, String> encrypted = map(bindingId).readAllMap();
        if (encrypted.size() > properties.getSession().getMaxAuthorizedClients()) {
            throw new IllegalStateException("Edge session AuthorizedClient limit exceeded");
        }
        List<OAuth2AuthorizedClient> clients = new ArrayList<>(encrypted.size());
        encrypted.keySet().forEach(field -> {
            OAuth2AuthorizedClient client = read(bindingId, field);
            if (client != null) {
                clients.add(client);
            }
        });
        return List.copyOf(clients);
    }

    private void remove(String bindingId, String field) {
        RMapCache<String, String> clients = map(bindingId);
        clients.fastRemove(field);
        if (clients.isEmpty()) {
            clients.delete();
        }
    }

    private RMapCache<String, String> map(String bindingId) {
        return redisson.getMapCache(MAP_PREFIX + bindingId, StringCodec.INSTANCE);
    }

    private static String bindingId(WebSession session, boolean create) {
        Object existing = session.getAttribute(SESSION_BINDING_ATTRIBUTE);
        if (existing instanceof String value && !value.isBlank()) {
            return value;
        }
        if (!create) {
            return null;
        }
        String value = UUID.randomUUID().toString();
        session.getAttributes().put(SESSION_BINDING_ATTRIBUTE, value);
        return value;
    }

    private String attributeKey(ClientRegistration registration, Authentication principal) {
        String issuer = issuer(registration);
        return digest(
                issuer,
                tenantId(issuer),
                registration.getClientId(),
                registration.getRegistrationId(),
                principal.getName());
    }

    private Duration absoluteRemaining(WebSession session) {
        Instant expires = session.getCreationTime().plus(properties.getSession().getAbsoluteTimeout());
        Duration remaining = Duration.between(clock.instant(), expires);
        if (remaining.isNegative() || remaining.isZero()) {
            throw new IllegalStateException("Edge BFF session has expired");
        }
        return remaining;
    }

    private boolean expired(WebSession session) {
        Instant creation = session.getCreationTime();
        return creation == null
                || !clock.instant().isBefore(creation.plus(properties.getSession().getAbsoluteTimeout()));
    }

    private static String context(String bindingId, String field) {
        return bindingId + ":" + field;
    }

    private static String issuer(ClientRegistration registration) {
        String issuer = registration.getProviderDetails().getIssuerUri();
        if (issuer == null) {
            Object metadataIssuer = registration.getProviderDetails().getConfigurationMetadata().get("issuer");
            issuer = metadataIssuer instanceof String value ? value : null;
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("Edge registration issuer is required");
        }
        URI parsed = URI.create(issuer);
        if (!parsed.isAbsolute() || parsed.getHost() == null || parsed.getUserInfo() != null
                || parsed.getQuery() != null || parsed.getFragment() != null) {
            throw new IllegalArgumentException("Edge registration issuer is invalid");
        }
        return parsed.normalize().toString().replaceAll("/$", "");
    }

    private static String tenantId(String issuer) {
        String path = URI.create(issuer).getPath();
        if ("/platform".equals(path)) {
            return "PLATFORM";
        }
        if (path == null || !path.matches("/tenants/[1-9][0-9]*")) {
            throw new IllegalArgumentException("Edge issuer tenant path is invalid");
        }
        return path.substring("/tenants/".length());
    }

    private static boolean sameOrigin(URI left, URI right) {
        return left != null && left.getScheme() != null && left.getHost() != null
                && left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && port(left) == port(right);
    }

    private static int port(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
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
}
