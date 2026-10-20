package top.egon.cola.component.yuheng.edge.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;
import top.egon.cola.component.yuheng.edge.config.domain.bo.EdgeRegistrationResourceBindingBO;
import top.egon.cola.component.yuheng.edge.service.domain.bo.EdgeSessionBindingBO;
import top.egon.cola.component.yuheng.edge.service.domain.enums.EdgeSessionBindingStateEnum;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.contract.IdpPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.reactive.ReactiveOAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;

/** Owns the authoritative encrypted identity, absolute deadline and fenced session lifecycle. */
@Slf4j
@Component("edgeSessionBindingComponent")
@RequiredArgsConstructor
public class EdgeSessionBindingComponent {
    public static final String BINDING_ID = "egon.bff.bindingId";
    public static final String GENERATION = "egon.bff.generation";
    public static final String RESOURCE_ALIAS = "egon.bff.resourceAlias";
    public static final String REGISTRATION_ID = "egon.bff.registrationId";
    public static final String ISSUER = "egon.bff.issuer";
    public static final String RESOURCE = "egon.bff.resource";
    public static final String INTENT = "egon.bff.intent";
    public static final String LEASE = EdgeSessionBindingComponent.class.getName() + ".lease";
    private static final String PURPOSE = "binding-v2";
    private static final String POINTER_PURPOSE = "session-binding-pointer-v2";
    private static final Set<String> FORBIDDEN_USER_CLAIMS = Set.of("sid", "session_id", "token_version", "resource_server_id", "resource",
            "resource_version", "nonce", "roles", "permissions", "capabilities", "dataScopes", "fieldPolicies", "authVersion", "sessionVersion", "contextVersion");

    private static final String POINTER_SCRIPT = """
            local value = redis.call('get', KEYS[1])
            if value then return value end
            redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[2], 'NX')
            return redis.call('get', KEYS[1])
            """;
    private static final String INITIALIZE_SCRIPT = """
            if redis.call('exists', KEYS[1]) == 1 then return 0 end
            redis.call('hset', KEYS[1], 'epoch', ARGV[1], 'generation', '1', 'fence', '1', 'state', 'NEW', 'deadline', ARGV[2], 'payload', ARGV[3])
            redis.call('pexpire', KEYS[1], ARGV[4])
            return 1
            """;
    private static final String ACQUIRE_SCRIPT = """
            if redis.call('hget', KEYS[1], 'epoch') ~= ARGV[1] or redis.call('hget', KEYS[1], 'generation') ~= ARGV[2] then return -1 end
            local state = redis.call('hget', KEYS[1], 'state')
            if state ~= 'NEW' and state ~= 'ACTIVE' then return -1 end
            if tonumber(redis.call('hget', KEYS[1], 'deadline') or '0') <= tonumber(ARGV[3]) then return -1 end
            if not redis.call('set', KEYS[2], ARGV[4], 'PX', ARGV[5], 'NX') then return 0 end
            return redis.call('hincrby', KEYS[1], 'fence', 1)
            """;
    private static final String RELEASE_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) end
            return 0
            """;
    private static final String UPDATE_SCRIPT = """
            if redis.call('get', KEYS[2]) ~= ARGV[1] or redis.call('hget', KEYS[1], 'epoch') ~= ARGV[2]
                or redis.call('hget', KEYS[1], 'generation') ~= ARGV[3] or redis.call('hget', KEYS[1], 'fence') ~= ARGV[4] then return 0 end
            local state = redis.call('hget', KEYS[1], 'state')
            if state ~= ARGV[5] then return 0 end
            redis.call('hset', KEYS[1], 'generation', ARGV[6], 'state', ARGV[7], 'payload', ARGV[8])
            return 1
            """;
    private static final String CLIENT_WRITE_SCRIPT = """
            if redis.call('get', KEYS[2]) ~= ARGV[1] or redis.call('hget', KEYS[1], 'epoch') ~= ARGV[2]
                or redis.call('hget', KEYS[1], 'generation') ~= ARGV[3] or redis.call('hget', KEYS[1], 'fence') ~= ARGV[4] then return 0 end
            local state = redis.call('hget', KEYS[1], 'state')
            if state ~= 'NEW' and state ~= 'ACTIVE' then return 0 end
            if tonumber(redis.call('hget', KEYS[1], 'deadline') or '0') <= tonumber(ARGV[5]) then return 0 end
            if redis.call('hget', KEYS[6], 'has-listeners') or redis.call('hget', KEYS[6], 'max-size') then return 0 end
            if redis.call('hexists', KEYS[3], ARGV[8]) == 0 and redis.call('hlen', KEYS[3]) >= tonumber(ARGV[7]) then return 0 end
            redis.call('zadd', KEYS[4], ARGV[6], ARGV[8])
            redis.call('zadd', KEYS[5], math.min(tonumber(ARGV[6]), tonumber(ARGV[5]) + tonumber(ARGV[10])), ARGV[8])
            local value = struct.pack('dLc0', tonumber(ARGV[10]), string.len(ARGV[9]), ARGV[9])
            redis.call('hset', KEYS[3], ARGV[8], value)
            local ttl = tonumber(ARGV[6]) - tonumber(ARGV[5])
            redis.call('pexpire', KEYS[3], ttl)
            redis.call('pexpire', KEYS[4], ttl)
            redis.call('pexpire', KEYS[5], ttl)
            redis.call('hset', KEYS[1], 'state', 'ACTIVE', 'payload', ARGV[11])
            return 1
            """;
    private static final String CLIENT_CLEAR_SCRIPT = """
            if redis.call('get', KEYS[2]) ~= ARGV[1] or redis.call('hget', KEYS[1], 'epoch') ~= ARGV[2]
                or redis.call('hget', KEYS[1], 'generation') ~= ARGV[3] or redis.call('hget', KEYS[1], 'fence') ~= ARGV[4]
                or (redis.call('hget', KEYS[1], 'state') ~= 'CLOSED' and redis.call('hget', KEYS[1], 'state') ~= 'CLOSING') then return 0 end
            redis.call('del', KEYS[3], KEYS[4], KEYS[5])
            return 1
            """;
    private static final String CLIENT_REMOVE_SCRIPT = """
            if redis.call('get', KEYS[2]) ~= ARGV[1] or redis.call('hget', KEYS[1], 'epoch') ~= ARGV[2]
                or redis.call('hget', KEYS[1], 'generation') ~= ARGV[3] or redis.call('hget', KEYS[1], 'fence') ~= ARGV[4]
                or redis.call('hget', KEYS[1], 'state') ~= 'ACTIVE' then return 0 end
            redis.call('hdel', KEYS[3], ARGV[5])
            redis.call('zrem', KEYS[4], ARGV[5])
            redis.call('zrem', KEYS[5], ARGV[5])
            return 1
            """;

    @Qualifier("redisson") private final RedissonClient redisson;
    @Qualifier("reactiveClientRegistrationRepository") private final ReactiveClientRegistrationRepository registrations;
    @Qualifier("edgeOAuthProperties") private final EdgeOAuthProperties properties;
    @Qualifier("edgeTokenCipher") private final EdgeTokenCipher cipher;
    @Qualifier("jacksonObjectMapper") private final ObjectMapper mapper;
    @Qualifier("edgeClock") private final Clock clock;
    @Qualifier("defaultValidator") private final Validator validator;
    @Qualifier("edgeTrustedIssuerRegistry") private final TrustedIssuerRegistry trustedIssuers;
    @Qualifier("edgeIdpStarterProperties") private final IdpStarterProperties idpProperties;
    @Qualifier("edgeAccessTokenVerifier") private final Function<String, VerifiedJwtBO> signatureVerifier;
    @Qualifier("edgeRevocationCheck") private final Function<VerifiedJwtBO, Mono<OAuthRevocationReader.Result>> revocations;
    @Qualifier("idpObservedVerificationComponent") private final IdpObservedVerificationComponent observations;
    @Qualifier("verifiedPrincipalConverter") private final VerifiedPrincipalConverter principalConverter;
    @Qualifier("edgeTenantIssuerAuthenticationManager") private final TenantIssuerAuthenticationManagerComponent issuerManager;
    @Qualifier("identityResourceServerStateReader") private final IdentityResourceServerStateReader resourceStates;
    @Qualifier("identityOAuthClientStateReader") private final IdentityOAuthClientStateReader clientStates;
    @Qualifier("edgeReactiveOAuthRevocationReader") private final ReactiveOAuthRevocationReader reactiveRevocations;

    @PostConstruct
    public void validateConfiguration() {
        properties.validate(); idpProperties.validate();
        new ValidationUtils(validator).validate(properties);
        if (!observations.enabled()) { throw new IllegalStateException("EDGE_SECURITY_OBSERVATION_REQUIRED"); }
        Set<String> realIds = new java.util.LinkedHashSet<>(); Set<String> clientIds = new java.util.LinkedHashSet<>();
        Set<String> routeIds = new java.util.LinkedHashSet<>(); Set<String> paths = new java.util.LinkedHashSet<>();
        Set<String> resourceIds = new java.util.LinkedHashSet<>(); Set<URI> resourceUris = new java.util.LinkedHashSet<>();
        properties.getResources().forEach((alias, resource) -> {
            resource.setIssuerRegistrationIds(resource.getIssuerRegistrationIds()).setRouteIds(resource.getRouteIds())
                    .setRequestPaths(resource.getRequestPaths()).setScopes(resource.getScopes());
            new ValidationUtils(validator).validate(resource);
            if (!resourceIds.add(resource.getResourceServerId()) || !resourceUris.add(resource.getResourceUri())) { throw new IllegalStateException("Edge resource catalog tuples are not unique"); }
            if (!properties.getAllowedReturnPaths().contains(resource.getReturnPath())) { throw new IllegalStateException("Edge resource return path is not allowlisted"); }
            resource.getRouteIds().forEach(route -> { if (!routeIds.add(route)) { throw new IllegalStateException("Edge resource route has multiple owners"); } });
            resource.getRequestPaths().forEach(path -> {
                if (paths.stream().anyMatch(existing -> overlaps(existing, path))) { throw new IllegalStateException("Edge resource path has multiple owners"); }
                paths.add(path);
            });
            resource.getIssuerRegistrationIds().forEach((issuer, id) -> {
                var trusted = trustedIssuers.findByIssuer(issuer).orElseThrow(() -> new IllegalStateException("Edge resource issuer is not trusted"));
                if (trusted.tenantId() == null || !realIds.add(id)) { throw new IllegalStateException("Edge resource registration is not uniquely tenant bound"); }
                if (properties.getResources().containsKey(id) && !(alias.equals(id) && resource.getDefaultRegistrationId().equals(id))) {
                    throw new IllegalStateException("Edge alias and real registration collide");
                }
                var registration = registrations.findByRegistrationId(id).block(Duration.ofSeconds(5));
                if (registration == null || !issuer.equals(registration.getProviderDetails().getIssuerUri())
                        || !AuthorizationGrantType.AUTHORIZATION_CODE.equals(registration.getAuthorizationGrantType())
                        || !ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(registration.getClientAuthenticationMethod())
                        || registration.getClientSecret() == null || registration.getClientSecret().isBlank()
                        || !clientIds.add(registration.getClientId()) || !resource.getScopes().equals(registration.getScopes())
                        || !(issuer + "/oauth2/authorize").equals(registration.getProviderDetails().getAuthorizationUri())
                        || !(issuer + "/oauth2/token").equals(registration.getProviderDetails().getTokenUri())
                        || !trusted.jwksUri().toASCIIString().equals(registration.getProviderDetails().getJwkSetUri())
                        || !(issuer + "/oauth2/revoke").equals(registration.getProviderDetails().getConfigurationMetadata().get("revocation_endpoint"))
                        || registration.getProviderDetails().getUserInfoEndpoint().getUri() != null
                                && !registration.getProviderDetails().getUserInfoEndpoint().getUri().isBlank()
                                && !(issuer + "/userinfo").equals(registration.getProviderDetails().getUserInfoEndpoint().getUri())
                        || !(properties.getPublicOrigin() + "/login/oauth2/code/{registrationId}").equals(registration.getRedirectUri())) {
                    throw new IllegalStateException("Edge real registration does not match its resource and confidential issuer tuple");
                }
            });
        });
        properties.setResources(java.util.Collections.unmodifiableMap(new LinkedHashMap<>(properties.getResources())));
        properties.setAllowedReturnPaths(List.copyOf(properties.getAllowedReturnPaths()));
        if (realIds.size() > 64 || !properties.getResources().containsKey(properties.getDefaultRegistrationId())) {
            throw new IllegalStateException("Edge configured resource catalog is not bounded or has no default alias");
        }
    }

    private static boolean overlaps(String first, String second) {
        String left = first.endsWith("/**") ? first.substring(0, first.length() - 3) : first;
        String right = second.endsWith("/**") ? second.substring(0, second.length() - 3) : second;
        return left.equals(right) || first.endsWith("/**") && right.startsWith(left + "/")
                || second.endsWith("/**") && left.startsWith(right + "/");
    }

    public String resolveRegistration(String alias, URI canonicalIssuer) {
        var resource = resource(alias);
        String id = canonicalIssuer == null ? resource.getDefaultRegistrationId() : resource.getIssuerRegistrationIds().get(canonicalIssuer.toASCIIString());
        if (id == null) { throw unavailable("Edge canonical issuer has no configured resource registration"); }
        return id;
    }

    public String aliasForRegistration(String realId) {
        List<String> owners = properties.getResources().entrySet().stream()
                .filter(entry -> entry.getValue().getIssuerRegistrationIds().containsValue(realId)).map(Map.Entry::getKey).toList();
        if (owners.size() != 1) { throw unavailable("Edge real registration is not uniquely configured"); }
        return owners.getFirst();
    }

    public EdgeRegistrationResourceBindingBO resource(String alias) {
        var value = properties.getResources().get(alias);
        if (value == null) { throw unavailable("Edge resource alias is not configured"); }
        return new ValidationUtils(validator).validate(value);
    }

    public boolean resourcesContainPath(String path) {
        var matcher = new org.springframework.util.AntPathMatcher();
        return properties.getResources().values().stream().flatMap(resource -> resource.getRequestPaths().stream())
                .anyMatch(pattern -> matcher.match(pattern, path));
    }

    public Mono<EdgeSessionBindingBO> getOrCreateForLogin(ServerWebExchange exchange) {
        return exchange.getSession().flatMap(session -> Mono.fromCallable(() -> {
            Object existing = session.getAttribute(BINDING_ID);
            if (existing instanceof String id) { return requireUsable(readState(id), expectedGeneration(session)); }
            String pointer = pointerKey(session.getId());
            var createdAt = clock.instant();
            var seed = EdgeSessionBindingBO.builder().bindingId(UUID.randomUUID().toString()).generation(1).fencingToken(1)
                    .originalCreatedAt(createdAt).absoluteExpiresAt(createdAt.plus(properties.getSession().getAbsoluteTimeout())).build();
            String proposed = encode(seed, POINTER_PURPOSE, pointer);
            String actual = eval(RScript.ReturnType.VALUE, POINTER_SCRIPT, List.of(pointer), proposed, remaining(seed));
            var initial = decode(actual, POINTER_PURPOSE, pointer);
            requireUsable(initial, initial.getGeneration());
            eval(RScript.ReturnType.INTEGER, INITIALIZE_SCRIPT, List.of(bindingKey(initial.getBindingId())), epoch(),
                    initial.getAbsoluteExpiresAt().toEpochMilli(), encode(initial, PURPOSE, initial.getBindingId()), retention(initial));
            var binding = requireUsable(readState(initial.getBindingId()), initial.getGeneration());
            session.getAttributes().put(BINDING_ID, binding.getBindingId()); session.getAttributes().put(GENERATION, binding.getGeneration());
            return binding;
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<EdgeSessionBindingBO> load(ServerWebExchange exchange) {
        return exchange.getSession().flatMap(session -> Mono.fromCallable(() -> {
            Object id = session.getAttribute(BINDING_ID);
            if (!(id instanceof String value)) { throw required(); }
            return requireUsable(readState(value), expectedGeneration(session));
        }).subscribeOn(Schedulers.boundedElastic()));
    }

    public <T> Mono<T> withLifecycle(ServerWebExchange exchange, Function<LifecycleLease, Mono<T>> work) {
        return withLifecycle(exchange, work, ignored -> Mono.empty());
    }

    public <T> Mono<T> withLifecycle(ServerWebExchange exchange, Function<LifecycleLease, Mono<T>> work,
            Function<LifecycleLease, Mono<Void>> cancelCleanup) {
        LifecycleLease current = exchange.getAttribute(LEASE);
        if (current != null) { return work.apply(current); }
        return load(exchange).flatMap(binding -> Mono.usingWhen(acquire(binding), lease -> {
            exchange.getAttributes().put(LEASE, lease);
            return work.apply(lease);
        }, this::release, (lease, failure) -> release(lease), lease -> cancelCleanup.apply(lease)
                .onErrorResume(failure -> {
                    log.warn("Edge cancellation cleanup is not confirmed; failureType={}", failure.getClass().getSimpleName());
                    return Mono.empty();
                }).then(release(lease))))
                .doFinally(signal -> exchange.getAttributes().remove(LEASE));
    }

    public Mono<LifecycleLease> acquire(@Valid EdgeSessionBindingBO binding) {
        new ValidationUtils(validator).validate(binding);
        String owner = UUID.randomUUID().toString();
        return Mono.defer(() -> Mono.fromCallable(() -> {
            Number fence = eval(RScript.ReturnType.INTEGER, ACQUIRE_SCRIPT, List.of(bindingKey(binding.getBindingId()), leaseKey(binding.getBindingId())),
                    epoch(), binding.getGeneration(), clock.millis(), owner, properties.getRefresh().getLockLease().toMillis());
            if (fence == null || fence.longValue() < 0) { throw required(); }
            return fence.longValue() == 0 ? null : new LifecycleLease(binding.getBindingId(), owner, binding.getGeneration(), fence.longValue());
        }).subscribeOn(Schedulers.boundedElastic()))
                .repeatWhenEmpty(3000, repeat -> repeat.delayElements(Duration.ofMillis(10)))
                .timeout(properties.getRefresh().getLockWait())
                .onErrorMap(TimeoutException.class, failure -> unavailable("Edge lifecycle lease is unavailable"));
    }

    public Mono<Void> release(LifecycleLease lease) {
        return Mono.fromRunnable(() -> eval(RScript.ReturnType.INTEGER, RELEASE_SCRIPT, List.of(leaseKey(lease.bindingId())), lease.owner()))
                .subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<EdgeSessionBindingBO> state(LifecycleLease lease) {
        return Mono.fromCallable(() -> {
            var value = requireUsable(readState(lease.bindingId()), lease.generation());
            if (value.getFencingToken() != lease.fence()) { throw unavailable("Edge lifecycle fence has changed"); }
            return value;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<EdgeSessionBindingBO> canonical(LifecycleLease lease, @Valid IdentityPrincipal principal, @Valid TokenAuditProofBO proof) {
        new ValidationUtils(validator).validate(principal);
        new ValidationUtils(validator).validate(proof);
        return state(lease).flatMap(binding -> {
            URI issuer = proof.issuer();
            var trusted = trustedIssuers.findByIssuer(issuer.toASCIIString()).orElseThrow(() -> invalid("Edge activation issuer is not trusted"));
            if (!principal.tenantId().equals(trusted.tenantId())) { return Mono.error(invalid("Edge activation tenant disagrees with its issuer")); }
            if (binding.getState() == EdgeSessionBindingStateEnum.ACTIVE
                    && (!issuer.equals(binding.getIssuer()) || !principal.tenantId().equals(binding.getTenantId()) || !principal.subject().equals(binding.getSubject()))) {
                return Mono.error(invalid("Edge canonical identity cannot change within a session"));
            }
            var next = copy(binding).setIssuer(issuer).setTenantId(principal.tenantId()).setSubject(principal.subject()).setState(EdgeSessionBindingStateEnum.ACTIVE);
            return Mono.just(new ValidationUtils(validator).validate(next));
        });
    }

    /** Commits first canonical activation and its encrypted native client in the same fenced Redis write. */
    public Mono<Void> writeClient(LifecycleLease lease, @Valid EdgeSessionBindingBO desired, String field, String encrypted) {
        new ValidationUtils(validator).validate(desired);
        if (desired.getState() != EdgeSessionBindingStateEnum.ACTIVE || !desired.getBindingId().equals(lease.bindingId())
                || desired.getGeneration() != lease.generation() || desired.getFencingToken() != lease.fence()
                || field == null || !field.matches("[0-9a-f]{64}") || encrypted == null || !encrypted.startsWith("ENC:v1:")) {
            return Mono.error(invalid("Edge client write tuple is invalid"));
        }
        return Mono.fromRunnable(() -> {
            String key = authorizedKey(lease.bindingId());
            // Pinned Redisson 3.26 RMapCache format: StringCodec field/value and native dLc0 idle envelope.
            Number changed = eval(RScript.ReturnType.INTEGER, CLIENT_WRITE_SCRIPT,
                    List.of(bindingKey(lease.bindingId()), leaseKey(lease.bindingId()), key,
                            org.redisson.RedissonObject.prefixName("redisson__timeout__set", key),
                            org.redisson.RedissonObject.prefixName("redisson__idle__set", key),
                            org.redisson.RedissonObject.suffixName(key, "redisson_options")),
                    lease.owner(), epoch(), lease.generation(), lease.fence(), clock.millis(), desired.getAbsoluteExpiresAt().toEpochMilli(),
                    properties.getSession().getMaxAuthorizedClients(), field, encrypted, properties.getSession().getIdleTimeout().toMillis(),
                    encode(desired, PURPOSE, lease.bindingId()));
            if (changed == null || changed.longValue() != 1) { throw unavailable("Edge client write fence rejected"); }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> beginClosing(LifecycleLease lease) {
        return state(lease).flatMap(binding -> {
            var closing = copy(binding).setGeneration(Math.addExact(binding.getGeneration(), 1)).setState(EdgeSessionBindingStateEnum.CLOSING).setClosedAt(clock.instant());
            return update(lease, binding, closing);
        });
    }

    public Mono<Void> finishClosing(LifecycleLease lease) {
        return Mono.fromCallable(() -> {
            var closing = readState(lease.bindingId());
            if (closing.getGeneration() != Math.addExact(lease.generation(), 1) || closing.getFencingToken() != lease.fence()
                    || closing.getState() != EdgeSessionBindingStateEnum.CLOSING) { throw unavailable("Edge terminal transition fence rejected"); }
            return closing;
        }).subscribeOn(Schedulers.boundedElastic()).flatMap(closing -> update(lease, closing, copy(closing).setState(EdgeSessionBindingStateEnum.CLOSED)));
    }

    public Mono<Void> close(LifecycleLease lease) { return beginClosing(lease).then(finishClosing(lease)); }

    public Mono<Void> clearClients(LifecycleLease lease) {
        return Mono.fromRunnable(() -> {
            String key = authorizedKey(lease.bindingId());
            Number changed = eval(RScript.ReturnType.INTEGER, CLIENT_CLEAR_SCRIPT,
                    List.of(bindingKey(lease.bindingId()), leaseKey(lease.bindingId()), key,
                            org.redisson.RedissonObject.prefixName("redisson__timeout__set", key),
                            org.redisson.RedissonObject.prefixName("redisson__idle__set", key)),
                    lease.owner(), epoch(), Math.addExact(lease.generation(), 1), lease.fence());
            if (changed == null || changed.longValue() != 1) { throw unavailable("Edge terminal client cleanup fence rejected"); }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> removeClient(LifecycleLease lease, String field) {
        if (field == null || !field.matches("[0-9a-f]{64}")) { return Mono.error(invalid("Edge client field is invalid")); }
        return Mono.fromRunnable(() -> {
            String key = authorizedKey(lease.bindingId());
            Number changed = eval(RScript.ReturnType.INTEGER, CLIENT_REMOVE_SCRIPT,
                    List.of(bindingKey(lease.bindingId()), leaseKey(lease.bindingId()), key,
                            org.redisson.RedissonObject.prefixName("redisson__timeout__set", key),
                            org.redisson.RedissonObject.prefixName("redisson__idle__set", key)),
                    lease.owner(), epoch(), lease.generation(), lease.fence(), field);
            if (changed == null || changed.longValue() != 1) { throw unavailable("Edge client removal fence rejected"); }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> rotatePointer(WebSession session, @Valid EdgeSessionBindingBO binding) {
        return Mono.fromRunnable(() -> {
            String key = pointerKey(session.getId());
            String candidate = encode(binding, POINTER_PURPOSE, key);
            String existing = eval(RScript.ReturnType.VALUE, POINTER_SCRIPT, List.of(key), candidate, remaining(binding));
            if (!decode(existing, POINTER_PURPOSE, key).getBindingId().equals(binding.getBindingId())) { throw unavailable("Edge rotated session pointer conflicts"); }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<AccessTokenVerification<IdentityPrincipal>> verifyClient(OAuth2AuthorizedClient client, Authentication authentication, String trace) {
        String alias = aliasForRegistration(client.getClientRegistration().getRegistrationId());
        var target = resource(alias); var registration = client.getClientRegistration();
        return Mono.fromCallable(() -> observations.observe(capture -> {
            var verified = new java.util.concurrent.atomic.AtomicReference<VerifiedJwtBO>();
            try {
                var result = fullVerifier(target).verifyAny(client.getAccessToken().getTokenValue(), verified::set);
                if (result instanceof AccessTokenVerification.Unavailable<?> unavailable) { return new AccessTokenVerification.Unavailable<IdentityPrincipal>(unavailable.reasonCode()); }
                if (result instanceof AccessTokenVerification.Expired<?>) { return new AccessTokenVerification.Expired<IdentityPrincipal>(); }
                if (result instanceof AccessTokenVerification.Invalid<?> invalid) { return new AccessTokenVerification.Invalid<IdentityPrincipal>(invalid.reasonCode()); }
                var valid = (AccessTokenVerification.Valid<IdpPrincipal>) result;
                var token = verified.get();
                if (!(valid.principal() instanceof IdentityPrincipal identity) || token == null || token.getTrustedIssuer().tenantId() == null
                        || !token.getTrustedIssuer().issuer().toASCIIString().equals(registration.getProviderDetails().getIssuerUri())
                        || !identity.subject().equals(authentication.getName()) || !identity.subject().equals(client.getPrincipalName())
                        || !registration.getClientId().equals(token.getJwt().getClaimAsString("client_id"))
                        || !token.getJwt().getAudience().equals(List.of(target.getResourceUri().toASCIIString()))
                        || FORBIDDEN_USER_CLAIMS.stream().anyMatch(token.getJwt()::hasClaim) || !target.getScopes().containsAll(valid.scopes())) {
                    return new AccessTokenVerification.Invalid<IdentityPrincipal>("JWT_RESOURCE_IDENTITY_INVALID");
                }
                capture.accept(token);
                return new AccessTokenVerification.Valid<>(identity, valid.scopes());
            } finally { verified.set(null); }
        }, target.getResourceUri(), trace)).subscribeOn(Schedulers.boundedElastic());
    }

    /** Uses the unchanged full SDK pipeline against this fixed deployment resource tuple. */
    public Mono<AccessTokenVerification<IdpPrincipal>> verifyExternal(String alias, String token, String trace) {
        var target = resource(alias);
        return Mono.fromCallable(() -> observations.observe(capture -> {
            var verified = new java.util.concurrent.atomic.AtomicReference<VerifiedJwtBO>();
            try {
                var result = fullVerifier(target).verifyAny(token, verified::set);
                if (result instanceof AccessTokenVerification.Valid<?> && verified.get() != null) {
                    if (!verified.get().getJwt().getAudience().equals(List.of(target.getResourceUri().toASCIIString()))) {
                        return new AccessTokenVerification.Invalid<IdpPrincipal>("JWT_AUDIENCE_INVALID");
                    }
                    capture.accept(verified.get());
                }
                return result;
            } finally { verified.set(null); }
        }, target.getResourceUri(), trace)).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Boolean> authenticationFailure(top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO marker) {
        return Mono.fromCallable(() -> marker.denyOnce(observations::authenticationFailure)).subscribeOn(Schedulers.boundedElastic());
    }

    private IdpJwtVerifier fullVerifier(EdgeRegistrationResourceBindingBO target) {
        return IdpJwtVerifier.createConfigured(null, resourceStates, clientStates, target.getResourceServerId(), target.getResourceUri(),
                idpProperties.getPlatformAudience(), clock, issuerManager, new WorkerRevocationReader(reactiveRevocations, idpProperties.getRevocation().getTimeout()),
                principalConverter, null);
    }

    @Slf4j
    @RequiredArgsConstructor
    private static class WorkerRevocationReader implements OAuthRevocationReader {
        @Qualifier("edgeReactiveOAuthRevocationReader") private final ReactiveOAuthRevocationReader delegate;
        private final Duration timeout;
        @Override public Result check(@Valid VerifiedJwtBO token) {
            if (Schedulers.isInNonBlockingThread()) { throw unavailable("Edge verification requires its bounded worker"); }
            return delegate.check(token).block(timeout.plusMillis(100));
        }
    }

    public String authorizedKey(String bindingId) { return "egon:bff:authorized-client:v2:{" + bindingId + "}"; }

    private Mono<Void> update(LifecycleLease lease, EdgeSessionBindingBO previous, EdgeSessionBindingBO next) {
        new ValidationUtils(validator).validate(next);
        return Mono.fromRunnable(() -> {
            Number changed = eval(RScript.ReturnType.INTEGER, UPDATE_SCRIPT, List.of(bindingKey(lease.bindingId()), leaseKey(lease.bindingId())), lease.owner(), epoch(),
                    previous.getGeneration(), lease.fence(), previous.getState().getCode(), next.getGeneration(), next.getState().getCode(), encode(next, PURPOSE, lease.bindingId()));
            if (changed == null || changed.longValue() != 1) { throw unavailable("Edge lifecycle write fence rejected"); }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private EdgeSessionBindingBO readState(String id) {
        if (id == null || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) { throw required(); }
        Map<String, String> stored;
        try { stored = redisson.<String, String>getMap(bindingKey(id), StringCodec.INSTANCE).readAllMap(); }
        catch (org.redisson.client.RedisException failure) { throw unavailable("Edge binding Redis is unavailable"); }
        if (!epoch().equals(stored.get("epoch")) || !stored.containsKey("payload")) { throw required(); }
        var binding = decode(stored.get("payload"), PURPOSE, id);
        if (!id.equals(binding.getBindingId()) || !Long.toString(binding.getGeneration()).equals(stored.get("generation"))
                || !binding.getState().getCode().equals(stored.get("state")) || !Long.toString(binding.getAbsoluteExpiresAt().toEpochMilli()).equals(stored.get("deadline"))) {
            throw unavailable("Edge encrypted binding metadata disagrees");
        }
        try { binding.setFencingToken(Long.parseLong(stored.get("fence"))); }
        catch (NumberFormatException invalid) { throw new OAuth2AuthenticationException(new OAuth2Error("temporarily_unavailable"), "Edge binding fence is invalid", invalid); }
        return new ValidationUtils(validator).validate(binding);
    }

    private EdgeSessionBindingBO requireUsable(EdgeSessionBindingBO binding, long generation) {
        if (binding.getGeneration() != generation || binding.getState() == EdgeSessionBindingStateEnum.CLOSED
                || binding.getState() == EdgeSessionBindingStateEnum.CLOSING || !clock.instant().isBefore(binding.getAbsoluteExpiresAt())) { throw required(); }
        return binding;
    }

    private long expectedGeneration(WebSession session) {
        Object value = session.getAttribute(GENERATION);
        if (!(value instanceof Number generation) || generation.longValue() < 1) { throw required(); }
        return generation.longValue();
    }

    private String encode(@Valid EdgeSessionBindingBO binding, String purpose, String context) {
        new ValidationUtils(validator).validate(binding);
        try { return new String(cipher.encrypt(mapper.writeValueAsBytes(binding), purpose, context), StandardCharsets.US_ASCII); }
        catch (com.fasterxml.jackson.core.JsonProcessingException failure) { throw unavailable("Edge binding encoding is unavailable"); }
    }

    private EdgeSessionBindingBO decode(String value, String purpose, String context) {
        if (value == null) { throw required(); }
        try { return new ValidationUtils(validator).validate(mapper.readValue(cipher.decrypt(value.getBytes(StandardCharsets.US_ASCII), purpose, context), EdgeSessionBindingBO.class)); }
        catch (java.io.IOException | IllegalArgumentException | jakarta.validation.ValidationException failure) {
            throw new OAuth2AuthenticationException(new OAuth2Error("temporarily_unavailable"), "Edge binding decoding is unavailable", failure);
        }
    }

    private EdgeSessionBindingBO copy(EdgeSessionBindingBO binding) {
        return binding.toBuilder().build();
    }

    private long remaining(EdgeSessionBindingBO binding) {
        long value = Duration.between(clock.instant(), binding.getAbsoluteExpiresAt()).toMillis();
        if (value <= 0) { throw required(); }
        return value;
    }

    private long retention(EdgeSessionBindingBO binding) { return Math.addExact(remaining(binding), properties.getRefresh().getLockLease().plus(properties.getRefresh().getTokenTimeout()).toMillis()); }
    private String epoch() { return Objects.requireNonNull(idpProperties.getRevocation().getEpoch(), "epoch"); }
    private static String bindingKey(String id) { return "egon:bff:binding:v2:{" + id + "}"; }
    private static String leaseKey(String id) { return "egon:bff:lifecycle:v2:{" + id + "}"; }
    private static String pointerKey(String session) { return "egon:bff:session-binding:v2:{" + digest(session) + "}"; }

    public static String digest(String... fields) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) { byte[] bytes = field.getBytes(StandardCharsets.UTF_8); digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array()); digest.update(bytes); }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("SHA-256 unavailable", failure); }
    }

    private <T> T eval(RScript.ReturnType type, String script, List<Object> keys, Object... values) {
        try { return redisson.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, script, type, keys, values); }
        catch (org.redisson.client.RedisException failure) { throw unavailable("Edge lifecycle Redis is unavailable"); }
    }

    public static OAuth2AuthenticationException invalid(String reason) { return new OAuth2AuthenticationException(new OAuth2Error("invalid_request"), reason); }
    public static OAuth2AuthenticationException unavailable(String reason) { return new OAuth2AuthenticationException(new OAuth2Error("temporarily_unavailable"), reason); }
    private static OAuth2AuthenticationException required() { return new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "Authoritative Edge session is required"); }

    public record LifecycleLease(String bindingId, String owner, long generation, long fence) { }
}
