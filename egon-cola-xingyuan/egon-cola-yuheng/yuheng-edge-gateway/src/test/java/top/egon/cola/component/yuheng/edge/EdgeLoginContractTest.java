package top.egon.cola.component.yuheng.edge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.redisson.api.RMapCache;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;
import top.egon.cola.component.yuheng.edge.config.EdgeSessionRedisConfiguration;
import top.egon.cola.component.yuheng.edge.config.EdgeSecurityConfiguration;
import top.egon.cola.component.yuheng.edge.controller.EdgeSessionController;
import top.egon.cola.component.yuheng.edge.controller.domain.EdgeSessionResult;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutIntentStore;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutRevocationIntent;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutService;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeAuthorizedClientRepository;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeOAuthTokenRevocationClient;
import top.egon.cola.component.yuheng.edge.config.EdgeSessionRedisSerializer;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeSingleFlightAuthorizedClientManager;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeTokenCipher;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeLoginContractTest {
    private static final String EDGE_PACKAGE = "top.egon.cola.component.yuheng.edge.";
    private static final Instant NOW = Instant.parse("2026-10-09T00:00:00Z");

    @TempDir
    Path tempDir;

    @Test
    void edgeDeclaresManagedRedisDriverAndExistingWebfluxOpenApiAdapter() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));

        assertThat(pom).contains("<artifactId>spring-boot-starter-data-redis</artifactId>");
        assertThat(pom).contains("<artifactId>yuheng-starter-openapi-webflux</artifactId>");
        assertThat(pom).contains("<artifactId>lombok</artifactId>");
        assertThat(pom).contains("<scope>provided</scope>");
        assertThat(pom).doesNotContain("<artifactId>lettuce-core</artifactId>");
        assertThat(GatewayProperties.PREFIX).isEqualTo("spring.cloud.gateway.server.webflux");
    }

    @Test
    void servletBackedIssuerVerifierCanInitializeWithoutAWebServer() {
        IdpStarterProperties properties = new IdpStarterProperties();
        TenantIssuerAuthenticationManagerComponent verifier = new TenantIssuerAuthenticationManagerComponent(
                new TrustedIssuerRegistry(properties),
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatCode(verifier::initialize).doesNotThrowAnyException();
    }

    @Test
    void authorizedClientRepositoryIsTheReactiveServerSessionAdapter() {
        Optional<Class<?>> repository = load("service.impl.EdgeAuthorizedClientRepository");

        assertThat(repository).as("session-scoped AuthorizedClient repository").isPresent();
        assertThat(ServerOAuth2AuthorizedClientRepository.class.isAssignableFrom(repository.orElseThrow()))
                .isTrue();
        assertThat(Arrays.stream(repository.orElseThrow().getDeclaredFields())
                .map(Field::getType)
                .map(Class::getSimpleName))
                .contains("RedissonClient", "EdgeTokenCipher");
    }

    @Test
    void refreshManagerDecoratesThePinnedReactiveManager() {
        Optional<Class<?>> manager = load("service.impl.EdgeSingleFlightAuthorizedClientManager");

        assertThat(manager).as("distributed BFF refresh manager").isPresent();
        assertThat(ReactiveOAuth2AuthorizedClientManager.class.isAssignableFrom(manager.orElseThrow()))
                .isTrue();
    }

    @Test
    void concurrentRelayRefreshesWaitForTheEncryptedRepositoryWrite() {
        EdgeOAuthProperties properties = propertiesWithIssuerOrigin();
        properties.getRefresh().setLockWait(Duration.ofSeconds(2));
        properties.getRefresh().setLockLease(Duration.ofSeconds(5));
        properties.getRefresh().setTokenTimeout(Duration.ofSeconds(2));
        ClientRegistration tenantA = registration("tenant-a", "https://idp.example/tenants/1001", "client-a");
        ClientRegistration tenantB = registration("tenant-b", "https://idp.example/tenants/1002", "client-b");
        Map<String, ClientRegistration> registrationsById = Map.of(
                tenantA.getRegistrationId(), tenantA,
                tenantB.getRegistrationId(), tenantB);
        ReactiveClientRegistrationRepository registrations = id -> Mono.justOrEmpty(registrationsById.get(id));
        TestingAuthenticationToken principal = new TestingAuthenticationToken(
                "subject-1", "unused", "ROLE_USER");
        WebSession session = mock(WebSession.class);
        when(session.getAttribute(EdgeAuthorizedClientRepository.SESSION_BINDING_ATTRIBUTE))
                .thenReturn("session-binding-1");
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getSession()).thenReturn(Mono.just(session));

        RedissonReactiveClient redisson = mock(RedissonReactiveClient.class);
        Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();
        Map<String, RLockReactive> locks = new ConcurrentHashMap<>();
        when(redisson.getLock(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return locks.computeIfAbsent(name, ignored -> lock(semaphores.computeIfAbsent(name,
                    unused -> new Semaphore(1, true))));
        });

        AtomicInteger refreshes = new AtomicInteger();
        Map<String, AtomicReference<OAuth2AuthorizedClient>> current = new ConcurrentHashMap<>();
        current.put(tenantA.getRegistrationId(), new AtomicReference<>(expiredAuthorizedClient(tenantA, principal.getName())));
        current.put(tenantB.getRegistrationId(), new AtomicReference<>(expiredAuthorizedClient(tenantB, principal.getName())));
        ReactiveOAuth2AuthorizedClientManager delegate = mock(ReactiveOAuth2AuthorizedClientManager.class);
        doAnswer(invocation -> {
            OAuth2AuthorizeRequest request = invocation.getArgument(0);
            AtomicReference<OAuth2AuthorizedClient> authorized = current.get(request.getClientRegistrationId());
            return Mono.defer(() -> {
                OAuth2AuthorizedClient existing = authorized.get();
                if (existing.getAccessToken().getExpiresAt().isAfter(NOW)) {
                    return Mono.just(existing);
                }
                return Mono.delay(Duration.ofMillis(20)).map(ignored -> {
                    refreshes.incrementAndGet();
                    OAuth2AuthorizedClient fresh = authorizedClient(
                            existing.getClientRegistration(),
                            existing.getPrincipalName(),
                            "fresh-" + request.getClientRegistrationId(),
                            "rotated-" + request.getClientRegistrationId());
                    authorized.set(fresh);
                    return fresh;
                });
            });
        }).when(delegate).authorize(any(OAuth2AuthorizeRequest.class));

        EdgeSingleFlightAuthorizedClientManager manager = new EdgeSingleFlightAuthorizedClientManager(
                delegate, registrations, redisson, properties);
        OAuth2AuthorizeRequest requestA = authorizeRequest(tenantA, principal, exchange);
        List<OAuth2AuthorizedClient> returned = Flux.range(0, 20)
                .flatMap(ignored -> manager.authorize(requestA), 20)
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(returned).hasSize(20);
        assertThat(refreshes).hasValue(1);
        assertThat(returned).extracting(value -> value.getAccessToken().getTokenValue())
                .containsOnly("fresh-tenant-a");
        manager.authorize(authorizeRequest(tenantB, principal, exchange)).block(Duration.ofSeconds(5));
        assertThat(refreshes).hasValue(2);
        assertThat(locks).hasSize(2);
        assertThat(locks.keySet()).allSatisfy(lockName ->
                assertThat(lockName).doesNotContain("subject-1", "client-secret", "raw-refresh"));
    }

    @Test
    void sessionRedisSerializerEncryptsTheWholeSessionObjectBoundary() {
        Optional<Class<?>> serializer = load("config.EdgeSessionRedisSerializer");

        assertThat(serializer).as("encrypted Spring Session serializer").isPresent();
        assertThat(RedisSerializer.class.isAssignableFrom(serializer.orElseThrow())).isTrue();
        assertThat(load("config.EdgeSessionRedisConfiguration"))
                .as("named Spring Session Redis configuration")
                .isPresent();
        assertThat(load("service.impl.EdgeTokenCipher")).as("mounted-key AES-GCM cipher").isPresent();
    }

    @Test
    void edgeCipherEncryptsAndAuthenticatesPurposeAndContext() throws Exception {
        EdgeTokenCipher cipher = newCipher();
        byte[] plaintext = "raw-refresh-token-value".getBytes(StandardCharsets.UTF_8);
        byte[] envelope = cipher.encrypt(plaintext, "logout-intent", "intent-1");

        assertThat(new String(envelope, StandardCharsets.US_ASCII)).doesNotContain("raw-refresh-token-value");
        assertThat(cipher.decrypt(envelope, "logout-intent", "intent-1")).containsExactly(plaintext);
        assertThatThrownBy(() -> cipher.decrypt(envelope, "logout-intent", "intent-2"))
                .isInstanceOf(IllegalArgumentException.class);

        byte[] altered = envelope.clone();
        altered[altered.length - 1] = (byte) (altered[altered.length - 1] ^ 1);
        assertThatThrownBy(() -> cipher.decrypt(altered, "logout-intent", "intent-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sessionSerializerDoesNotStorePlaintextAndRejectsTampering() throws Exception {
        EdgeTokenCipher cipher = newCipher();
        EdgeSessionRedisSerializer serializer = new EdgeSessionRedisSerializer(cipher);
        String secret = "id-token-and-refresh-token-values";

        byte[] encrypted = serializer.serialize(secret);
        assertThat(new String(encrypted, StandardCharsets.US_ASCII)).doesNotContain(secret);
        assertThat(serializer.deserialize(encrypted)).isEqualTo(secret);

        byte[] altered = encrypted.clone();
        altered[altered.length - 1] = (byte) (altered[altered.length - 1] ^ 1);
        assertThatThrownBy(() -> serializer.deserialize(altered))
                .isInstanceOf(org.springframework.data.redis.serializer.SerializationException.class);
    }

    @Test
    void logoutAtomicallyQueuesEverySessionRefreshTokenBeforeInvalidating() {
        ClientRegistration tenantA = registration("tenant-a", "https://idp.example/tenants/1001", "client-a");
        ClientRegistration tenantB = registration("tenant-b", "https://idp.example/tenants/1002", "client-b");
        OAuth2AuthorizedClient clientA = authorizedClient(tenantA, "subject-1", "access-A", "refresh-A");
        OAuth2AuthorizedClient clientB = authorizedClient(tenantB, "subject-1", "access-B", "refresh-B");
        OAuth2AuthenticationToken principal = oauthAuthentication("tenant-a", "subject-1");
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        EdgeAuthorizedClientRepository authorizedClients = mock(EdgeAuthorizedClientRepository.class);
        when(authorizedClients.loadAll(principal, exchange)).thenReturn(Mono.just(List.of(clientA, clientB)));
        EdgeLogoutIntentStore intentStore = mock(EdgeLogoutIntentStore.class);
        when(intentStore.enqueueAll(any())).thenReturn(Mono.just(List.of("1-0", "2-0")));
        when(intentStore.acknowledge(any())).thenReturn(Mono.empty());
        EdgeOAuthTokenRevocationClient revocation = mock(EdgeOAuthTokenRevocationClient.class);
        when(revocation.revoke(any())).thenReturn(Mono.empty());
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSession session = mock(WebSession.class);
        when(exchange.getResponse()).thenReturn(response);
        when(exchange.getSession()).thenReturn(Mono.just(session));
        when(session.invalidate()).thenReturn(Mono.empty());
        when(authorizedClients.removeAll(principal, exchange)).thenReturn(Mono.empty());
        EdgeLogoutService service = new EdgeLogoutService(
                authorizedClients, intentStore, revocation, Clock.fixed(NOW, ZoneOffset.UTC));

        EdgeLogoutService.RemoteState outcome = service.logout(principal, exchange).block();

        assertThat(outcome).isEqualTo(EdgeLogoutService.RemoteState.CONFIRMED);
        ArgumentCaptor<List<EdgeLogoutRevocationIntent>> intents = ArgumentCaptor.forClass(List.class);
        verify(intentStore).enqueueAll(intents.capture());
        assertThat(intents.getValue()).hasSize(2)
                .extracting(EdgeLogoutRevocationIntent::tokenValue)
                .containsExactlyInAnyOrder("refresh-A", "refresh-B");
        assertThat(intents.getValue())
                .extracting(EdgeLogoutRevocationIntent::tenantId)
                .containsExactly("1001", "1002");
        assertThat(intents.getValue()).allSatisfy(intent ->
                assertThat(intent.toString()).doesNotContain(intent.tokenValue()));
        verify(authorizedClients).removeAll(principal, exchange);
        verify(session).invalidate();
        verify(revocation, times(2)).revoke(any());
        verify(intentStore, times(2)).acknowledge(any());
    }

    @Test
    void logoutQueueFailureStillClearsLocalSessionAndReturnsUnknown() {
        ClientRegistration registration = registration("tenant-a", "https://idp.example/tenants/1001", "client-a");
        OAuth2AuthorizedClient client = authorizedClient(registration, "subject-1", "access", "refresh");
        OAuth2AuthenticationToken principal = oauthAuthentication("tenant-a", "subject-1");
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        EdgeAuthorizedClientRepository authorizedClients = mock(EdgeAuthorizedClientRepository.class);
        when(authorizedClients.loadAll(principal, exchange)).thenReturn(Mono.just(List.of(client)));
        EdgeLogoutIntentStore intentStore = mock(EdgeLogoutIntentStore.class);
        when(intentStore.enqueueAll(any())).thenReturn(Mono.error(new IllegalStateException("queue unavailable")));
        EdgeOAuthTokenRevocationClient revocation = mock(EdgeOAuthTokenRevocationClient.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSession session = mock(WebSession.class);
        when(exchange.getResponse()).thenReturn(response);
        when(exchange.getSession()).thenReturn(Mono.just(session));
        when(session.invalidate()).thenReturn(Mono.empty());
        when(authorizedClients.removeAll(principal, exchange)).thenReturn(Mono.empty());
        EdgeLogoutService service = new EdgeLogoutService(
                authorizedClients, intentStore, revocation, Clock.fixed(NOW, ZoneOffset.UTC));

        EdgeLogoutService.RemoteState outcome = service.logout(principal, exchange).block();

        assertThat(outcome).isEqualTo(EdgeLogoutService.RemoteState.UNKNOWN);
        verify(session).invalidate();
        verify(authorizedClients).removeAll(principal, exchange);
        verify(revocation, never()).revoke(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void authorizedClientsAreEncryptedAndPartitionedByRegistrationIdentity() throws Exception {
        EdgeTokenCipher cipher = newCipher();
        EdgeOAuthProperties properties = propertiesWithIssuerOrigin();
        ClientRegistration tenantA = registration("tenant-a", "https://idp.example/tenants/1001", "client-a");
        ClientRegistration tenantB = registration("tenant-b", "https://idp.example/tenants/1002", "client-b");
        Map<String, ClientRegistration> registrationsById = Map.of(
                tenantA.getRegistrationId(), tenantA,
                tenantB.getRegistrationId(), tenantB);
        ReactiveClientRegistrationRepository registrations = id -> Mono.justOrEmpty(registrationsById.get(id));
        RedissonClient redisson = mock(RedissonClient.class);
        RMapCache<String, String> map = mock(RMapCache.class);
        org.mockito.Mockito.doReturn(map).when(redisson).getMapCache(any(), eq(StringCodec.INSTANCE));
        Map<String, String> storedEntries = new ConcurrentHashMap<>();
        doAnswer(invocation -> {
            storedEntries.put(invocation.getArgument(0), invocation.getArgument(1));
            return true;
        }).when(map).fastPut(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS),
                anyLong(), eq(TimeUnit.SECONDS));
        when(map.get(anyString())).thenAnswer(invocation -> storedEntries.get(invocation.getArgument(0)));
        EdgeAuthorizedClientRepository repository = new EdgeAuthorizedClientRepository(
                redisson,
                registrations,
                properties,
                cipher,
                Clock.fixed(NOW, ZoneOffset.UTC));

        TestingAuthenticationToken principal = new TestingAuthenticationToken(
                "subject-1", "unused", "ROLE_USER");
        WebSession session = mock(WebSession.class);
        Map<String, Object> sessionAttributes = new java.util.HashMap<>();
        when(session.getAttributes()).thenReturn(sessionAttributes);
        when(session.getAttribute(any())).thenAnswer(invocation ->
                sessionAttributes.get(invocation.getArgument(0)));
        when(session.getCreationTime()).thenReturn(NOW.minusSeconds(5));
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getSession()).thenReturn(Mono.just(session));

        OAuth2AuthorizedClient clientA = authorizedClient(tenantA, principal.getName(),
                "raw-access-A", "raw-refresh-A");
        OAuth2AuthorizedClient clientB = authorizedClient(tenantB, principal.getName(),
                "raw-access-B", "raw-refresh-B");
        repository.saveAuthorizedClient(clientA, principal, exchange).block();
        repository.saveAuthorizedClient(clientB, principal, exchange).block();

        ArgumentCaptor<String> fields = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ciphertexts = ArgumentCaptor.forClass(String.class);
        verify(map, times(2)).fastPut(
                fields.capture(), ciphertexts.capture(),
                anyLong(), eq(TimeUnit.SECONDS), anyLong(), eq(TimeUnit.SECONDS));
        assertThat(fields.getAllValues()).doesNotHaveDuplicates();
        assertThat(ciphertexts.getAllValues()).allSatisfy(value -> {
            assertThat(value).doesNotContain("raw-access", "raw-refresh");
        });

        when(map.readAllMap()).thenReturn(storedEntries);
        List<OAuth2AuthorizedClient> loaded = repository.loadAll(principal, exchange).block();
        assertThat(loaded).hasSize(2);
        assertThat(loaded).extracting(value -> value.getAccessToken().getTokenValue())
                .containsExactlyInAnyOrder("raw-access-A", "raw-access-B");
    }

    @Test
    void logoutIntentQueueAndWorkerExposeDurableRetryBoundaries() {
        Optional<Class<?>> store = load("service.EdgeLogoutIntentStore");
        Optional<Class<?>> redisStore = load("service.impl.RedissonEdgeLogoutIntentStore");
        Optional<Class<?>> worker = load("service.impl.EdgeLogoutRevocationWorker");
        Optional<Class<?>> service = load("service.EdgeLogoutService");

        assertThat(store).as("logout intent store port").isPresent();
        assertThat(redisStore).as("Redisson Redis Stream adapter").isPresent();
        assertThat(worker).as("pending-message recovery worker").isPresent();
        assertThat(service).as("logout orchestration").isPresent();
        Set<String> storeMethods = Arrays.stream(store.orElseThrow().getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertThat(storeMethods).contains("enqueue", "enqueueAll", "pollNewAndClaimStale", "acknowledge");
    }

    @Test
    void sessionControllerDefinesOnlyTheDocumentedBffOperations() throws Exception {
        Optional<Class<?>> controller = load("controller.EdgeSessionController");

        assertThat(controller).as("BFF session controller").isPresent();
        assertThat(controller.orElseThrow().getAnnotation(RequestMapping.class).value())
                .containsExactly("/bff");
        List<String> routes = Arrays.stream(controller.orElseThrow().getDeclaredMethods())
                .flatMap(method -> {
                    GetMapping get = method.getAnnotation(GetMapping.class);
                    if (get != null) {
                        return Arrays.stream(get.value());
                    }
                    PostMapping post = method.getAnnotation(PostMapping.class);
                    return post == null ? java.util.stream.Stream.empty() : Arrays.stream(post.value());
                })
                .toList();
        assertThat(routes).containsExactlyInAnyOrder("/session", "/csrf", "/logout");
        assertThat(Arrays.stream(controller.orElseThrow().getDeclaredMethods())
                .map(method -> method.getAnnotation(Operation.class))
                .filter(java.util.Objects::nonNull)
                .map(Operation::operationId))
                .contains("oauthBffSession", "oauthBffCsrf", "oauthBffLogout");
        Method sessionHandler = controller.orElseThrow()
                .getDeclaredMethod("session", Authentication.class, ServerWebExchange.class);
        assertThat(sessionHandler.getAnnotation(SecurityRequirement.class).name())
                .isEqualTo("bffSessionCookie");
        assertThat(Arrays.stream(sessionHandler.getAnnotation(ApiResponses.class).value())
                .map(ApiResponse::responseCode))
                .contains("200", "401", "503");
        Method csrfHandler = controller.orElseThrow().getDeclaredMethod("csrf", ServerWebExchange.class);
        assertThat(Arrays.stream(csrfHandler.getAnnotation(ApiResponses.class).value())
                .map(ApiResponse::responseCode))
                .containsExactly("200");
        Method logoutHandler = controller.orElseThrow()
                .getDeclaredMethod("logout", Authentication.class, ServerWebExchange.class);
        assertThat(Arrays.stream(logoutHandler.getAnnotation(SecurityRequirements.class).value())
                .map(SecurityRequirement::name))
                .containsExactly("bffSessionCookie", "bffCsrfHeader");
        ApiResponse empty204 = Arrays.stream(logoutHandler.getAnnotation(ApiResponses.class).value())
                .filter(response -> "204".equals(response.responseCode()))
                .findFirst()
                .orElseThrow();
        assertThat(empty204.content()).isEmpty();
        Set<String> controllerDependencies = Arrays.stream(controller.orElseThrow().getDeclaredFields())
                .map(Field::getType)
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        assertThat(controllerDependencies).contains("Function");
        assertThat(controllerDependencies).doesNotContain("TenantIssuerAuthenticationManagerComponent",
                "ReactiveOAuthRevocationReader");
        assertThat(controllerDependencies).doesNotContain("ReactiveOAuth2AuthorizedClientManager");
        Class<?> sessionResult = load("controller.domain.EdgeSessionResult").orElseThrow();
        assertThat(Arrays.stream(sessionResult.getRecordComponents())
                .map(component -> component.getName()))
                .containsExactly("authenticated", "tenantId", "subject", "displayName");
        assertThat(load("controller.domain.EdgeCsrfResult")).isPresent();
        assertThat(load("controller.domain.EdgeLogoutFailureResult")).isPresent();
    }

    @Test
    void api015VerifiesTheHeldTenantTokenAndMapsSharedStateWithoutRefreshing() {
        String issuer = "https://idp.example/tenants/1001";
        String audience = "https://api.example/resource";
        ClientRegistration registration = registration("tenant-a", issuer, "client-a");
        ReactiveClientRegistrationRepository registrations = id -> Mono.just(registration);
        DefaultOAuth2User user = new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "subject-1", "name", "Edge User"),
                "sub");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                user, user.getAuthorities(), registration.getRegistrationId());
        OAuth2AuthorizedClient client = authorizedClient(
                registration, authentication.getName(), "active-access", "active-refresh");
        EdgeAuthorizedClientRepository authorizedClients = mock(EdgeAuthorizedClientRepository.class);
        AtomicReference<OAuth2AuthorizedClient> sessionClient = new AtomicReference<>(client);
        WebSession session = mock(WebSession.class);
        when(session.getAttribute(EdgeSessionController.SESSION_CREATED_AT_ATTRIBUTE))
                .thenReturn(NOW.minusSeconds(30));
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getSession()).thenReturn(Mono.just(session));
        when(authorizedClients.loadAuthorizedClient(registration.getRegistrationId(), authentication, exchange))
                .thenAnswer(ignored -> Mono.just(sessionClient.get()));

        TrustedIssuerRegistry.TrustedIssuerVO trustedIssuer = new TrustedIssuerRegistry.TrustedIssuerVO(
                URI.create(issuer),
                URI.create("https://idp.example/tenants/1001/oauth2/jwks"),
                "tenant-1001",
                TrustedIssuerRegistry.RealmType.TENANT,
                "1001",
                TrustedIssuerRegistry.Status.ACTIVE,
                1L);
        Jwt jwt = Jwt.withTokenValue("active-access")
                .header("alg", "RS256")
                .issuer(issuer)
                .subject("subject-1")
                .audience(List.of(audience))
                .issuedAt(NOW.minusSeconds(30))
                .expiresAt(NOW.plusSeconds(300))
                .build();
        VerifiedJwtBO verified = mock(VerifiedJwtBO.class);
        when(verified.getTrustedIssuer()).thenReturn(trustedIssuer);
        when(verified.getPrincipalType()).thenReturn(PrincipalType.USER);
        AtomicReference<String> verifiedTenantId = new AtomicReference<>("1001");
        when(verified.getTenantId()).thenAnswer(ignored -> verifiedTenantId.get());
        AtomicReference<String> verifiedSubject = new AtomicReference<>("subject-1");
        when(verified.getSubject()).thenAnswer(ignored -> verifiedSubject.get());
        AtomicReference<Jwt> verifiedJwt = new AtomicReference<>(jwt);
        when(verified.getJwt()).thenAnswer(ignored -> verifiedJwt.get());
        AtomicInteger verifierCalls = new AtomicInteger();
        AtomicReference<OAuthRevocationReader.Result> revocationResult =
                new AtomicReference<>(OAuthRevocationReader.Result.valid());
        java.util.function.Function<String, VerifiedJwtBO> verifier = token -> {
            assertThat(token).isEqualTo("active-access");
            verifierCalls.incrementAndGet();
            return verified;
        };
        java.util.function.Function<VerifiedJwtBO, Mono<OAuthRevocationReader.Result>> revocationReader =
                ignored -> Mono.just(revocationResult.get());
        IdpStarterProperties idpProperties = new IdpStarterProperties();
        idpProperties.setResourceUri(URI.create(audience));

        EdgeSessionController controller = new EdgeSessionController(
                authorizedClients,
                mock(EdgeLogoutService.class),
                registrations,
                propertiesWithIssuerOrigin(),
                idpProperties,
                verifier,
                revocationReader,
                Clock.fixed(NOW, ZoneOffset.UTC));

        ResponseEntity<?> valid = controller.session(authentication, exchange).block();
        assertThat(valid.getStatusCode().value()).isEqualTo(200);
        assertThat(valid.getBody()).isInstanceOf(EdgeSessionResult.class);
        assertThat(valid.getHeaders().getCacheControl()).contains("no-store");

        verifiedJwt.set(Jwt.withTokenValue("active-access")
                .header("alg", "RS256")
                .issuer("https://other.example/tenants/1001")
                .subject("subject-1")
                .audience(List.of(audience))
                .issuedAt(NOW.minusSeconds(30))
                .expiresAt(NOW.plusSeconds(300))
                .build());
        assertThat(controller.session(authentication, exchange).block().getStatusCode().value()).isEqualTo(401);

        verifiedJwt.set(Jwt.withTokenValue("different-access")
                .header("alg", "RS256")
                .issuer(issuer)
                .subject("subject-1")
                .audience(List.of(audience))
                .issuedAt(NOW.minusSeconds(30))
                .expiresAt(NOW.plusSeconds(300))
                .build());
        assertThat(controller.session(authentication, exchange).block().getStatusCode().value()).isEqualTo(401);

        verifiedJwt.set(Jwt.withTokenValue("active-access")
                .header("alg", "RS256")
                .issuer(issuer)
                .subject("another-subject")
                .audience(List.of(audience))
                .issuedAt(NOW.minusSeconds(30))
                .expiresAt(NOW.plusSeconds(300))
                .build());
        assertThat(controller.session(authentication, exchange).block().getStatusCode().value()).isEqualTo(401);

        verifiedJwt.set(Jwt.withTokenValue("active-access")
                .header("alg", "RS256")
                .issuer(issuer)
                .subject("subject-1")
                .audience(List.of("https://api.example/other"))
                .issuedAt(NOW.minusSeconds(30))
                .expiresAt(NOW.plusSeconds(300))
                .build());
        assertThat(controller.session(authentication, exchange).block().getStatusCode().value()).isEqualTo(401);

        verifiedJwt.set(jwt);
        verifiedSubject.set("another-subject");
        assertThat(controller.session(authentication, exchange).block().getStatusCode().value()).isEqualTo(401);
        verifiedSubject.set("subject-1");

        verifiedTenantId.set("1002");
        assertThat(controller.session(authentication, exchange).block().getStatusCode().value()).isEqualTo(401);
        verifiedTenantId.set("1001");

        sessionClient.set(expiredAuthorizedClient(registration, authentication.getName()));
        assertThat(controller.session(authentication, exchange).block().getStatusCode().value()).isEqualTo(401);
        sessionClient.set(client);

        revocationResult.set(OAuthRevocationReader.Result.revoked("TOKEN_REVOKED"));
        assertThat(controller.session(authentication, exchange).block().getStatusCode().value()).isEqualTo(401);

        revocationResult.set(OAuthRevocationReader.Result.unavailable("OAUTH_SECURITY_STATE_UNAVAILABLE"));
        assertThat(controller.session(authentication, exchange).block().getStatusCode().value()).isEqualTo(503);
        assertThat(verifierCalls.get()).isEqualTo(9);
    }

    @Test
    void logoutRequiresAnAuthenticatedBffSession() {
        EdgeLogoutService logoutService = mock(EdgeLogoutService.class);
        EdgeSessionController controller = new EdgeSessionController(
                mock(EdgeAuthorizedClientRepository.class),
                logoutService,
                registrationId -> Mono.empty(),
                propertiesWithIssuerOrigin(),
                new IdpStarterProperties(),
                token -> null,
                ignored -> Mono.empty(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        ResponseEntity<?> response = controller.logout(null, mock(ServerWebExchange.class)).block();

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(logoutService);
    }

    @Test
    void browserChainRequiresSessionAndCsrfWithoutRedirectingApiRequests() {
        EdgeOAuthProperties properties = propertiesWithIssuerOrigin();
        EdgeSecurityConfiguration configuration = new EdgeSecurityConfiguration(properties);
        InMemoryReactiveClientRegistrationRepository registrations =
                new InMemoryReactiveClientRegistrationRepository(registration(
                        "tenant-a", "https://idp.example/tenants/1001", "client-a"));
        ServerOAuth2AuthorizedClientRepository authorizedClients =
                mock(ServerOAuth2AuthorizedClientRepository.class);
        WebSessionServerCsrfTokenRepository csrfTokens = new WebSessionServerCsrfTokenRepository();
        SecurityWebFilterChain apiChain = configuration.edgeApiSecurityWebFilterChain(
                ServerHttpSecurity.http(), csrfTokens);
        SecurityWebFilterChain browserChain = configuration.edgeBrowserSecurityWebFilterChain(
                ServerHttpSecurity.http(),
                registrations,
                authorizedClients,
                csrfTokens,
                Clock.fixed(NOW, ZoneOffset.UTC));
        WebFilterChainProxy security = new WebFilterChainProxy(apiChain, browserChain);
        WebHandler handler = exchange -> {
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.NO_CONTENT);
            return exchange.getResponse().setComplete();
        };
        WebTestClient client = WebTestClient.bindToWebHandler(handler)
                .webFilter(security)
                .build();

        client.get().uri("/bff/session")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("Cache-Control", "no-store");
        client.post().uri("/bff/logout")
                .exchange()
                .expectStatus().isForbidden();
        client.post().uri("/bff/logout")
                .header("Origin", "https://edge.example")
                .exchange()
                .expectStatus().isUnauthorized();
        client.get().uri("/api/test")
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().doesNotExist("Location");
    }

    @Test
    void springSessionRedisFactoryRequiresTheExactPrimaryAndLocalPlaintextBoundary() {
        EdgeSessionRedisConfiguration configuration = new EdgeSessionRedisConfiguration();
        RedisProperties properties = new RedisProperties();
        Environment environment = mock(Environment.class);
        when(environment.acceptsProfiles("local")).thenReturn(false);
        properties.setUrl("rediss://redis.example:6380");

        RedisConnectionFactory factory = configuration.edgeSessionRedisConnectionFactory(
                properties,
                "rediss://redis.example:6380",
                environment);
        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
        ((LettuceConnectionFactory) factory).destroy();

        assertThatThrownBy(() -> configuration.edgeSessionRedisConnectionFactory(
                properties,
                "rediss://redis.example:6381",
                environment))
                .isInstanceOf(IllegalStateException.class);

        properties.setUrl("redis://redis.example:6379");
        assertThatThrownBy(() -> configuration.edgeSessionRedisConnectionFactory(
                properties,
                "redis://redis.example:6379",
                environment))
                .isInstanceOf(IllegalStateException.class);

        when(environment.acceptsProfiles("local")).thenReturn(true);
        properties.setUrl("redis://127.0.0.1:6379");
        RedisConnectionFactory localFactory = configuration.edgeSessionRedisConnectionFactory(
                properties,
                "redis://127.0.0.1:6379",
                environment);
        assertThat(localFactory).isInstanceOf(LettuceConnectionFactory.class);
        ((LettuceConnectionFactory) localFactory).destroy();
    }

    @Test
    void edgePropertiesBoundOriginKeyIdAndExternalTimeouts() {
        EdgeOAuthProperties properties = propertiesWithIssuerOrigin();
        properties.getSession().getEncryption().setKeyId("session-key-v1");
        properties.getSession().getEncryption().setKeyFile("/mounted/session-key");
        properties.validate();

        properties.getLogout().setPollInterval(Duration.ofMinutes(2));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.getLogout().setPollInterval(Duration.ofSeconds(5));
        properties.getLogout().setClaimIdle(Duration.ofMinutes(11));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.getLogout().setClaimIdle(Duration.ofSeconds(30));
        properties.getLogout().setTimeout(Duration.ofSeconds(31));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.getLogout().setTimeout(Duration.ofSeconds(5));
        properties.getRefresh().setLockWait(Duration.ofSeconds(31));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.getRefresh().setLockWait(Duration.ofSeconds(5));
        properties.getRefresh().setLockLease(Duration.ofSeconds(61));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.getRefresh().setLockLease(Duration.ofSeconds(15));
        properties.getRefresh().setTokenTimeout(Duration.ofSeconds(31));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.getRefresh().setTokenTimeout(Duration.ofSeconds(5));

        properties.setPublicOrigin(URI.create("https://edge.example/"));
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
        properties.setPublicOrigin(URI.create("https://edge.example"));

        properties.getSession().getEncryption().setKeyId("invalid:key-id");
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bothProfilesRequireTheSameEncryptedSessionAndDurableLogoutConfiguration() throws Exception {
        Path resources = Path.of("src/main/resources");
        assertThat(Files.exists(resources.resolve("application.yml"))).isTrue();
        assertThat(Files.exists(resources.resolve("application-local.yml"))).isTrue();

        String base = Files.readString(resources.resolve("application.yml"));
        String local = Files.readString(resources.resolve("application-local.yml"));
        for (String marker : List.of("namespace:",
                "url:",
                "public-origin:",
                "default-registration-id:",
                "upstreams:",
                "session:",
                "idle-timeout:",
                "encryption:",
                "key-id:",
                "key-file:",
                "refresh:",
                "lock-wait:",
                "lock-lease:",
                "token-timeout:",
                "trusted-issuers:",
                "jwks-uri:",
                "resource-uri:",
                "primary-endpoint:",
                "epoch:",
                "logout:",
                "consumer-group:",
                "max-pending:",
                "api-docs:",
                "group-configs:",
                "published-groups:")) {
            assertThat(base).as("base profile key %s", marker).contains(marker);
            assertThat(local).as("local profile key %s", marker).contains(marker);
        }
        Properties baseProperties = loadYaml(resources.resolve("application.yml"));
        Properties localProperties = loadYaml(resources.resolve("application-local.yml"));
        assertThat(baseProperties.stringPropertyNames())
                .isEqualTo(localProperties.stringPropertyNames())
                .contains("spring.session.redis.namespace", "spring.data.redis.url",
                        "egon.yuheng.edge.session.encryption.key-file",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.edge-ui.issuer",
                        "egon.cola.platform.tianquan.shoubing.revocation.epoch",
                        "egon.cola.component.yuheng.openapi.enabled",
                        "springdoc.api-docs.enabled",
                        "springdoc.group-configs[0].group");
        assertThat(baseProperties.getProperty("egon.cola.component.yuheng.openapi.enabled"))
                .isEqualTo("${EGON_YUHENG_EDGE_OPENAPI_ENABLED:false}");
        assertThat(baseProperties.getProperty("springdoc.api-docs.enabled"))
                .isEqualTo("${EGON_YUHENG_EDGE_OPENAPI_ENABLED:false}");
        assertThat(baseProperties.getProperty("springdoc.group-configs[0].group"))
                .isEqualTo("edge-bff");
        assertThat(baseProperties.getProperty("egon.cola.component.yuheng.openapi.published-groups[0]"))
                .isEqualTo("edge-bff");
        assertGatewayRoutes(resources.resolve("application.yml"));
        assertGatewayRoutes(resources.resolve("application-local.yml"));
    }

    @Test
    void bffRoutesHaveOasMetadataAndSecurityConfigurationPreservesApiNoRedirect() {
        Optional<Class<?>> controller = load("controller.EdgeSessionController");
        Optional<Class<?>> security = load("config.EdgeSecurityConfiguration");

        assertThat(controller).as("real annotated BFF routes").isPresent();
        assertThat(security).as("separate browser and API security chains").isPresent();
        assertThat(load("config.EdgeOAuthProperties")).isPresent();
        assertThat(load("filter.TrustedIdentityHeaderFilter")).isPresent();
    }

    @Test
    void managedEdgeAdaptersHaveStableNamesAndQualifiedLombokConstructorParameters() throws Exception {
        assertThat(load("service.impl.EdgeTokenCipher").orElseThrow()
                .getAnnotation(Component.class).value()).isEqualTo("edgeTokenCipher");
        assertThat(load("service.impl.EdgeAuthorizedClientRepository").orElseThrow()
                .getAnnotation(Component.class).value()).isEqualTo("edgeAuthorizedClientRepository");
        assertThat(load("config.EdgeSessionRedisSerializer").orElseThrow()
                .getAnnotation(Component.class).value()).isEqualTo("edgeSessionRedisSerializer");
        assertThat(load("service.impl.RedissonEdgeLogoutIntentStore").orElseThrow()
                .getAnnotation(Component.class).value()).isEqualTo("edgeLogoutIntentStore");
        assertThat(load("service.impl.EdgeOAuthTokenRevocationClient").orElseThrow()
                .getAnnotation(Component.class).value()).isEqualTo("edgeOAuthTokenRevocationClient");
        assertThat(load("service.impl.EdgeLogoutRevocationWorker").orElseThrow()
                .getAnnotation(Component.class).value()).isEqualTo("edgeLogoutRevocationWorker");
        assertThat(EdgeLogoutService.class.getAnnotation(Service.class).value())
                .isEqualTo("edgeLogoutService");
        assertThat(EdgeSessionController.class.getAnnotation(RestController.class).value())
                .isEqualTo("edgeSessionController");
        assertThat(EdgeSecurityConfiguration.class.getAnnotation(Configuration.class).value())
                .isEqualTo("edgeSecurityConfiguration");
        assertThat(EdgeOAuthProperties.class.getAnnotation(Component.class).value())
                .isEqualTo("edgeOAuthProperties");

        for (Class<?> type : List.of(
                EdgeTokenCipher.class,
                EdgeAuthorizedClientRepository.class,
                EdgeSessionRedisSerializer.class,
                top.egon.cola.component.yuheng.edge.service.impl.RedissonEdgeLogoutIntentStore.class,
                EdgeOAuthTokenRevocationClient.class,
                top.egon.cola.component.yuheng.edge.service.impl.EdgeLogoutRevocationWorker.class,
                EdgeLogoutService.class,
                EdgeSecurityConfiguration.class,
                EdgeSessionController.class)) {
            assertQualifiedConstructor(type);
        }

    }

    private Optional<Class<?>> load(String typeName) {
        try {
            return Optional.of(Class.forName(EDGE_PACKAGE + typeName));
        } catch (ClassNotFoundException exception) {
            return Optional.empty();
        }
    }

    private static void assertQualifiedConstructor(Class<?> type) {
        java.lang.reflect.Constructor<?> constructor = Arrays.stream(type.getDeclaredConstructors())
                .max(java.util.Comparator.comparingInt(java.lang.reflect.Constructor::getParameterCount))
                .orElseThrow();
        assertThat(constructor.getParameterCount()).as("constructor dependencies for %s", type.getSimpleName())
                .isGreaterThan(0);
        for (java.lang.reflect.Parameter parameter : constructor.getParameters()) {
            assertThat(parameter.isAnnotationPresent(Qualifier.class))
                    .as("Qualifier for %s.%s", type.getSimpleName(), parameter.getName())
                    .isTrue();
        }
    }

    private EdgeTokenCipher newCipher() throws Exception {
        Path keyFile = tempDir.resolve("edge-kek-" + java.util.UUID.randomUUID());
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 41);
        Files.write(keyFile, key);
        Files.setPosixFilePermissions(keyFile, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
        EdgeOAuthProperties properties = new EdgeOAuthProperties();
        properties.getSession().getEncryption().setKeyId("test-key-v1");
        properties.getSession().getEncryption().setKeyFile(keyFile.toString());
        EdgeTokenCipher cipher = new EdgeTokenCipher(properties);
        cipher.initializeKey();
        return cipher;
    }

    private static EdgeOAuthProperties propertiesWithIssuerOrigin() {
        EdgeOAuthProperties properties = new EdgeOAuthProperties();
        properties.getUpstreams().setAuthorizationServer(URI.create("https://idp.example"));
        properties.getUpstreams().setBizGateway(URI.create("https://biz.example"));
        properties.getUpstreams().setMcpGateway(URI.create("https://mcp.example"));
        properties.setPublicOrigin(URI.create("https://edge.example"));
        properties.setDefaultRegistrationId("tenant-a");
        return properties;
    }

    private static ClientRegistration registration(String registrationId, String issuer, String clientId) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId(clientId)
                .clientSecret("client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://edge.example/login/oauth2/code/{registrationId}")
                .scope("openid", "offline_access")
                .authorizationUri(issuer + "/oauth2/authorize")
                .tokenUri(issuer + "/oauth2/token")
                .jwkSetUri(issuer + "/oauth2/jwks")
                .issuerUri(issuer)
                .userNameAttributeName("sub")
                .clientName(registrationId)
                .build();
    }

    private static OAuth2AuthenticationToken oauthAuthentication(String registrationId, String subject) {
        DefaultOAuth2User user = new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", subject),
                "sub");
        return new OAuth2AuthenticationToken(
                user, user.getAuthorities(), registrationId);
    }

    private static OAuth2AuthorizedClient authorizedClient(
            ClientRegistration registration,
            String principalName,
            String accessTokenValue,
            String refreshTokenValue) {
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessTokenValue,
                NOW.minusSeconds(30),
                NOW.plusSeconds(300),
                Set.of("openid", "offline_access"));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(refreshTokenValue, NOW.minusSeconds(30));
        return new OAuth2AuthorizedClient(registration, principalName, accessToken, refreshToken);
    }

    private static OAuth2AuthorizedClient expiredAuthorizedClient(
            ClientRegistration registration,
            String principalName) {
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "expired-access-token",
                NOW.minusSeconds(600),
                NOW.minusSeconds(1),
                Set.of("openid", "offline_access"));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("raw-refresh-token", NOW.minusSeconds(60));
        return new OAuth2AuthorizedClient(registration, principalName, accessToken, refreshToken);
    }

    private static OAuth2AuthorizeRequest authorizeRequest(
            ClientRegistration registration,
            Authentication principal,
            ServerWebExchange exchange) {
        return OAuth2AuthorizeRequest.withClientRegistrationId(registration.getRegistrationId())
                .principal(principal)
                .attribute(ServerWebExchange.class.getName(), exchange)
                .build();
    }

    private static RLockReactive lock(Semaphore semaphore) {
        RLockReactive lock = mock(RLockReactive.class);
        doAnswer(invocation -> Mono.fromCallable(() -> semaphore.tryAcquire(
                        invocation.getArgument(0), TimeUnit.MILLISECONDS))
                .subscribeOn(Schedulers.boundedElastic()))
                .when(lock).tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS), anyLong());
        doAnswer(invocation -> Mono.fromRunnable(semaphore::release)
                .subscribeOn(Schedulers.boundedElastic()))
                .when(lock).unlock(anyLong());
        return lock;
    }

    private void assertGatewayRoutes(Path yamlPath) throws Exception {
        Properties properties = loadYaml(yamlPath);

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty(GatewayProperties.PREFIX + ".routes[0].id"))
                .isEqualTo("authorization-server");
        assertThat(properties.getProperty(GatewayProperties.PREFIX + ".routes[1].id"))
                .isEqualTo("yuheng-biz");
        assertThat(properties.getProperty(GatewayProperties.PREFIX + ".routes[1].filters[0]"))
                .isEqualTo("TokenRelay");
        assertThat(properties.getProperty(GatewayProperties.PREFIX + ".routes[2].id"))
                .isEqualTo("yuheng-mcp");
        assertThat(properties.getProperty(GatewayProperties.PREFIX + ".routes[2].filters[0]"))
                .isEqualTo("TokenRelay");
        assertThat(properties.getProperty("spring.cloud.gateway.routes[0].id")).isNull();
    }

    private Properties loadYaml(Path yamlPath) throws Exception {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ByteArrayResource(Files.readAllBytes(yamlPath)));
        Properties properties = yaml.getObject();
        return properties;
    }
}
