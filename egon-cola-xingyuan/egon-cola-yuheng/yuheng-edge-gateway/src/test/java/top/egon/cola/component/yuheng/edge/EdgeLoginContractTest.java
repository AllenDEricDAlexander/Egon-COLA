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
import org.springframework.web.util.UriComponentsBuilder;
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
    void nativeLoginAndCallbackAreInTheExistingEdgeOpenApiContract() {
        var api = new EdgeSecurityConfiguration(propertiesWithIssuerOrigin()).edgeOpenAPI();
        assertThat(api.getPaths()).containsKeys("/oauth2/authorization/{registrationId}",
                "/login/oauth2/code/{registrationId}");
        var login = api.getPaths().get("/oauth2/authorization/{registrationId}").getGet();
        var callback = api.getPaths().get("/login/oauth2/code/{registrationId}").getGet();
        assertThat(login.getOperationId()).isEqualTo("oauthBffLogin");
        assertThat(callback.getOperationId()).isEqualTo("oauthBffCallback");
        assertThat(login.getResponses()).containsKeys("302", "400");
        assertThat(callback.getResponses()).containsKeys("303", "400", "503");
        assertThat(api.getComponents().getSecuritySchemes()).containsKeys("bffSessionCookie", "bffCsrfHeader");
    }

    @Test
    void successfulApiAndMcpForwardingDropsCookiesButPreservesResolvedSessionAndBearer() throws Exception {
        try (var fixture = new EdgeResourceAuthorizationContractTest.Fixture(tempDir)) {
            var session = fixture.newSession();
            for (String alias : List.of("app-a", "app-b")) {
                var callback = fixture.pendingExchange(alias, session);
                fixture.clients.saveAuthorizedClient(fixture.client(alias, "user-1"), fixture.authentication(alias, "user-1"), callback).block();
                var reached = new AtomicReference<ServerWebExchange>();
                fixture.forward(alias, session, fixture.authentication(alias, "user-1"), null, reached);
                assertThat(reached.get()).isNotNull();
                assertThat(reached.get().getRequest().getHeaders().getFirst("Cookie")).isNull();
                assertThat(reached.get().getRequest().getHeaders().getFirst("Authorization")).startsWith("Bearer ");
                assertThat(reached.get().getRequest().getHeaders().getFirst("tenant_id")).isEqualTo("1001");
                assertThat(reached.get().getRequest().getHeaders().getFirst("x-egon-user-id")).isNull();
                String token = fixture.token(alias, "SERVICE", "client-" + alias, Map.of()); reached.set(null);
                fixture.forward(alias, fixture.newSession(), null, token, reached);
                assertThat(reached.get().getRequest().getHeaders().getFirst("Authorization")).isEqualTo("Bearer " + token);
                assertThat(reached.get().getRequest().getHeaders().getFirst("Cookie")).isNull();
            }
            assertThat(fixture.refreshExchanges).hasValue(0);
        }
    }

    @Test
    void asCookiesArePreservedWhileRejectedApiCredentialsNeverReachEngine() throws Exception {
        var filter = new top.egon.cola.component.yuheng.edge.filter.TrustedIdentityHeaderFilter(
                new com.fasterxml.jackson.databind.ObjectMapper());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        var as = org.springframework.mock.web.server.MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/tenants/1001/login")
                        .header("Cookie", "JSESSIONID=as-session"));
        filter.filter(as, next -> { forwarded.set(next); return Mono.empty(); }).block();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("Cookie"))
                .isEqualTo("JSESSIONID=as-session");
        forwarded.set(null);
        var mixed = org.springframework.mock.web.server.MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/api/orders")
                        .header("Authorization", "Bearer explicit-token")
                        .cookie(new org.springframework.http.HttpCookie("__Host-egon_bff", "session")));
        filter.filter(mixed, next -> { forwarded.set(next); return Mono.empty(); }).block();
        assertThat(forwarded.get()).isNull();
        assertThat(mixed.getResponse().getStatusCode().value()).isEqualTo(400);
        assertThat(new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(mixed.getResponse().getBodyAsString().block()).get("error").asText())
                .isEqualTo("invalid_request");
        var anonymous = org.springframework.mock.web.server.MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/mcp"));
        filter.filter(anonymous, next -> { forwarded.set(next); return Mono.empty(); }).block();
        assertThat(forwarded.get()).isNull();
        assertThat(anonymous.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(anonymous.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
    }

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
    void concurrentRelayRefreshesWaitForTheEncryptedRepositoryWrite() throws Exception {
        try (var fixture = new EdgeResourceAuthorizationContractTest.Fixture(tempDir)) {
            var session = fixture.newSession(); var callback = fixture.pendingExchange("app-a", session);
            var valid = fixture.client("app-a", "user-1");
            var expiredMetadata = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, valid.getAccessToken().getTokenValue(),
                    EdgeResourceAuthorizationContractTest.Fixture.TIME.minusSeconds(600), EdgeResourceAuthorizationContractTest.Fixture.TIME.minusSeconds(1), valid.getAccessToken().getScopes());
            fixture.clients.saveAuthorizedClient(new OAuth2AuthorizedClient(valid.getClientRegistration(), "user-1", expiredMetadata, valid.getRefreshToken()),
                    fixture.authentication("app-a", "user-1"), callback).block();
            var manager = fixture.manager(); var principal = fixture.authentication("app-a", "user-1");
            var returned = Flux.range(0, 20).flatMap(index -> manager.authorize(OAuth2AuthorizeRequest.withClientRegistrationId("app-a")
                    .principal(principal).attribute(ServerWebExchange.class.getName(), fixture.request("/api/app-a/data", session)).build()), 20)
                    .collectList().block(Duration.ofSeconds(10));
            assertThat(returned).hasSize(20); assertThat(fixture.refreshExchanges).hasValue(1);
            assertThat(returned).extracting(client -> client.getRefreshToken().getTokenValue()).containsOnly(returned.getFirst().getRefreshToken().getTokenValue());
            assertThat(fixture.leaseOwners).isEmpty();
        }
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
    void logoutAtomicallyQueuesEverySessionRefreshTokenBeforeInvalidating() throws Exception {
        try (var fixture = new EdgeResourceAuthorizationContractTest.Fixture(tempDir)) {
            var session = fixture.newSession(); ServerWebExchange callback = null;
            for (String alias : List.of("app-a", "app-b")) {
                callback = fixture.pendingExchange(alias, session);
                fixture.clients.saveAuthorizedClient(fixture.client(alias, "user-1"), fixture.authentication(alias, "user-1"), callback).block();
            }
            assertThat(fixture.logout(fixture.authentication("app-b", "user-1"), callback)).isEqualTo(EdgeLogoutService.RemoteState.CONFIRMED);
            assertThat(fixture.intents).hasSize(2).extracting(EdgeLogoutRevocationIntent::tenantId).containsOnly("1001");
            assertThat(fixture.intents).allSatisfy(intent -> assertThat(intent.toString()).doesNotContain(intent.tokenValue()));
            assertThat(fixture.remoteRevocations).hasValue(2); assertThat(fixture.leaseOwners).isEmpty();
            assertThat(callback.getResponse().getCookies().getFirst("__Host-egon_bff").getMaxAge()).isEqualTo(Duration.ZERO);
        }
    }

    @Test
    void logoutQueueFailureStillClearsLocalSessionAndReturnsUnknown() throws Exception {
        try (var fixture = new EdgeResourceAuthorizationContractTest.Fixture(tempDir)) {
            var callback = fixture.pendingExchange("app-a", fixture.newSession());
            fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), fixture.authentication("app-a", "user-1"), callback).block();
            fixture.failQueue = true;
            assertThat(fixture.logout(fixture.authentication("app-a", "user-1"), callback)).isEqualTo(EdgeLogoutService.RemoteState.UNKNOWN);
            assertThat(fixture.cached.values()).allSatisfy(map -> assertThat(map).isEmpty());
            assertThat(fixture.remoteRevocations).hasValue(0); assertThat(fixture.leaseOwners).isEmpty();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void authorizedClientsAreEncryptedAndPartitionedByRegistrationIdentity() throws Exception {
        try (var fixture = new EdgeResourceAuthorizationContractTest.Fixture(tempDir)) {
            var session = fixture.newSession(); var principal = fixture.authentication("app-a", "user-1");
            for (String id : List.of("app-a", "app-b")) {
                var callback = fixture.pendingExchange(id, session); fixture.clients.saveAuthorizedClient(fixture.client(id, "user-1"), principal, callback).block();
            }
            var exchange = fixture.request("/bff/session", session);
            assertThat(fixture.clients.loadAll(principal, exchange).block()).hasSize(2);
            assertThat(fixture.cached.values()).flatExtracting(Map::values).hasSize(2).allSatisfy(value -> assertThat(value).startsWith("ENC:v1:").doesNotContain("controlled-refresh"));
            assertThatThrownBy(() -> fixture.clients.loadAuthorizedClient("app-a-b", principal, exchange).block()).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> fixture.clients.loadAuthorizedClient("app-a", fixture.authentication("app-a", "different-user"), exchange).block()).isInstanceOf(RuntimeException.class);
        }
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
                .contains("200", "400", "503");
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
    void api015VerifiesTheHeldTenantTokenAndMapsSharedStateWithoutRefreshing() throws Exception {
        try (var fixture = new EdgeResourceAuthorizationContractTest.Fixture(tempDir)) {
            var session = fixture.newSession(); var principal = fixture.authentication("app-a", "user-1");
            var callback = fixture.pendingExchange("app-a", session); fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), principal, callback).block();
            var exchange = fixture.request("/bff/session", session); var controller = fixture.controller();
            ResponseEntity<?> valid = controller.session(principal, exchange).block();
            assertThat(valid.getStatusCode().value()).isEqualTo(200); assertThat(valid.getBody()).isInstanceOf(EdgeSessionResult.class);
            assertThat(valid.getHeaders().getCacheControl()).contains("no-store");
            fixture.snapshot.set(List.of("R", "JWT_REVOKED")); assertThat(controller.session(principal, exchange).block().getStatusCode().value()).isEqualTo(401);
            fixture.snapshot.set(List.of("U", "OAUTH_SECURITY_READY_ABSENT")); assertThat(controller.session(principal, exchange).block().getStatusCode().value()).isEqualTo(503);
            assertThat(fixture.codeExchanges).hasValue(0); assertThat(fixture.refreshExchanges).hasValue(0);
        }
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
    void browserChainRequiresSessionAndCsrfWithoutRedirectingApiRequests() throws Exception {
        try (var fixture = new EdgeResourceAuthorizationContractTest.Fixture(tempDir)) {
            var configuration = new EdgeSecurityConfiguration(fixture.properties); var csrf = new WebSessionServerCsrfTokenRepository();
            var api = configuration.edgeApiSecurityWebFilterChain(ServerHttpSecurity.http(), csrf);
            var browser = configuration.edgeBrowserSecurityWebFilterChain(ServerHttpSecurity.http(), fixture.registrations, fixture.clients, csrf,
                    EdgeResourceAuthorizationContractTest.Fixture.TIME_CLOCK, fixture.idp, fixture.mapper);
            var security = new WebFilterChainProxy(api, browser);
            WebTestClient client = WebTestClient.bindToWebHandler(exchange -> { exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.NO_CONTENT); return exchange.getResponse().setComplete(); }).webFilter(security).build();
            for (String path : List.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
                client.get().uri(path).exchange().expectStatus().isNoContent()
                        .expectHeader().doesNotExist("Location").expectHeader().doesNotExist("Set-Cookie");
            }
            client.get().uri("/login/oauth2/code/app-a?code=synthetic-code&state=missing-request").exchange().expectStatus().isBadRequest().expectHeader().doesNotExist("Location");
            client.get().uri("/bff/session").exchange().expectStatus().isUnauthorized().expectHeader().valueEquals("Cache-Control", "no-store");
            client.post().uri("/bff/logout").exchange().expectStatus().isForbidden();
            client.post().uri("/bff/logout").header("Origin", "https://edge.example").exchange().expectStatus().isUnauthorized();
            client.get().uri("/api/test").exchange().expectStatus().isNoContent().expectHeader().doesNotExist("Location");
            client.post().uri("/api/app-a/data").cookie("__Host-egon_bff", "opaque")
                    .header("Authorization", "Bearer external-token").exchange().expectStatus().isBadRequest().expectHeader().doesNotExist("Location");
            URI normal = client.get().uri("/oauth2/authorization/app-a").exchange().expectStatus().isFound().returnResult(Void.class).getResponseHeaders().getLocation();
            var normalQuery = UriComponentsBuilder.fromUri(normal).build().getQueryParams(); assertThat(normalQuery.getFirst("prompt")).isNull();
            URI stepUp = client.get().uri("/oauth2/authorization/app-a?stepUp=true").exchange().expectStatus().isFound().returnResult(Void.class).getResponseHeaders().getLocation();
            var stepUpQuery = UriComponentsBuilder.fromUri(stepUp).build().getQueryParams(); assertThat(stepUpQuery.getFirst("prompt")).isEqualTo("login"); assertThat(stepUpQuery.getFirst("max_age")).isEqualTo("0");
            client.get().uri("/oauth2/authorization/app-a?resource=https://attacker.example/resource").exchange().expectStatus().isBadRequest();
            client.get().uri("/oauth2/authorization/app-a?stepUp=trusted-input").exchange().expectStatus().isBadRequest();
        }
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
    void edgePrimaryRedissonUsesTheSharedEndpointDatabaseAndCredentials() throws Exception {
        EdgeSessionRedisConfiguration configuration = new EdgeSessionRedisConfiguration();
        RedisProperties properties = new RedisProperties();
        properties.setUrl("rediss://redis.example:6380");
        properties.setDatabase(3);
        properties.setUsername("edge-test");
        properties.setPassword("fixture-secret");
        properties.setTimeout(Duration.ofMillis(750));
        RedissonClient primary = mock(RedissonClient.class);
        Environment environment = mock(Environment.class);
        try (var redisson = org.mockito.Mockito.mockStatic(org.redisson.Redisson.class)) {
            ArgumentCaptor<org.redisson.config.Config> config =
                    ArgumentCaptor.forClass(org.redisson.config.Config.class);
            redisson.when(() -> org.redisson.Redisson.create(any(org.redisson.config.Config.class)))
                    .thenReturn(primary);

            assertThat(configuration.edgePrimaryRedisson(properties, properties.getUrl(), environment))
                    .isSameAs(primary);

            redisson.verify(() -> org.redisson.Redisson.create(config.capture()));
            var server = config.getValue().useSingleServer();
            assertThat(server.getAddress()).isEqualTo(properties.getUrl());
            assertThat(server.getDatabase()).isEqualTo(3);
            assertThat(server.getUsername()).isEqualTo("edge-test");
            assertThat(server.getPassword()).isEqualTo("fixture-secret");
            assertThat(server.getTimeout()).isEqualTo(750);
        }
        var bean = EdgeSessionRedisConfiguration.class.getMethod("edgePrimaryRedisson",
                RedisProperties.class, String.class, Environment.class)
                .getAnnotation(org.springframework.context.annotation.Bean.class);
        assertThat(bean.name()).containsExactly("redisson");
        assertThat(bean.destroyMethod()).isEqualTo("shutdown");
    }

    @Test
    void edgePrimaryRedissonRejectsEndpointDriftAndNonlocalPlaintextBeforeConnecting() {
        EdgeSessionRedisConfiguration configuration = new EdgeSessionRedisConfiguration();
        RedisProperties properties = new RedisProperties();
        properties.setUrl("rediss://redis.example:6380");
        Environment environment = mock(Environment.class);
        assertThatThrownBy(() -> configuration.edgePrimaryRedisson(properties,
                "rediss://redis.example:6381", environment)).isInstanceOf(IllegalStateException.class);
        properties.setUrl("redis://127.0.0.1:6379");
        assertThatThrownBy(() -> configuration.edgePrimaryRedisson(properties,
                properties.getUrl(), environment)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void edgePropertiesBoundOriginKeyIdAndExternalTimeouts() {
        EdgeOAuthProperties properties = propertiesWithIssuerOrigin();
        properties.getSession().getEncryption().setKeyId("session-key-v1");
        properties.getSession().getEncryption().setKeyFile("/mounted/session-key");
        properties.setResources(Map.of("tenant-a", top.egon.cola.component.yuheng.edge.config.domain.bo.EdgeRegistrationResourceBindingBO.builder()
                .resourceServerId("resource-1").resourceUri(URI.create("https://api.example/resource")).defaultRegistrationId("tenant-a")
                .issuerRegistrationIds(Map.of("https://idp.example/tenants/1001", "tenant-a")).scopes(Set.of("openid", "offline_access")).returnPath("/").build()));
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
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.edge-issuer-a.issuer",
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
                EdgeSessionController.class,
                top.egon.cola.component.yuheng.edge.filter.TrustedIdentityHeaderFilter.class)) {
            assertQualifiedConstructor(type);
        }

    }

    @Test
    void generatedOpenApiHasFiveOriginalOperationsExactSchemasAndLogoutAndSecurity() throws Exception {
        try (var fixture = new EdgeResourceAuthorizationContractTest.Fixture(tempDir);
                var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.setEnvironment(new org.springframework.boot.web.reactive.context.StandardReactiveWebEnvironment());
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("edgeDocsFixture", Map.of(
                    "springdoc.api-docs.enabled", true, "springdoc.api-docs.version", "OPENAPI_3_1",
                    "egon.cola.component.yuheng.openapi.enabled", false, "egon.cola.component.id.enabled", false,
                    "egon.cola.platform.tianquan.shoubing.enabled", false,
                    "spring.cloud.gateway.server.webflux.filter.json-to-grpc.enabled", false,
                    "spring.autoconfigure.exclude", "org.redisson.spring.starter.RedissonAutoConfigurationV2,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration")));
            context.registerBean("edgeSessionController", EdgeSessionController.class, fixture::controller);
            var config = new EdgeSecurityConfiguration(fixture.properties);
            context.registerBean("edgeOpenAPI", io.swagger.v3.oas.models.OpenAPI.class, config::edgeOpenAPI);
            context.registerBean("edgeBffOpenApiSecurityCustomizer", org.springdoc.core.customizers.GlobalOpenApiCustomizer.class, config::edgeBffOpenApiSecurityCustomizer);
            context.register(EdgeDocumentFixtureConfiguration.class); context.refresh();
            String body = WebTestClient.bindToApplicationContext(context).build().get().uri("/v3/api-docs/edge-bff").exchange().expectStatus().isOk()
                    .expectBody(String.class).returnResult().getResponseBody();
            var doc = fixture.mapper.readTree(body); assertThat(doc.path("openapi").asText()).startsWith("3.1.");
            var paths = doc.path("paths");
            assertThat(paths.path("/oauth2/authorization/{registrationId}").path("get").path("operationId").asText()).isEqualTo("oauthBffLogin");
            var callback = paths.path("/login/oauth2/code/{registrationId}").path("get");
            assertThat(callback.path("operationId").asText()).isEqualTo("oauthBffCallback");
            assertThat(callback.path("responses").has("303")).isTrue(); assertThat(callback.path("responses").has("503")).isTrue();
            var issuer = java.util.stream.StreamSupport.stream(callback.path("parameters").spliterator(), false).filter(param -> "iss".equals(param.path("name").asText())).findFirst().orElseThrow();
            assertThat(issuer.path("required").asBoolean()).isTrue(); assertThat(issuer.path("schema").path("maxLength").asInt()).isEqualTo(2048);
            assertThat(paths.path("/bff/session").path("get").path("operationId").asText()).isEqualTo("oauthBffSession");
            assertThat(paths.path("/bff/csrf").path("get").path("operationId").asText()).isEqualTo("oauthBffCsrf");
            var logout = paths.path("/bff/logout").path("post"); assertThat(logout.path("operationId").asText()).isEqualTo("oauthBffLogout");
            assertThat(logout.path("security")).hasSize(1); assertThat(logout.path("security").get(0).has("bffSessionCookie")).isTrue();
            assertThat(logout.path("security").get(0).has("bffCsrfHeader")).isTrue();
            assertThat(logout.path("responses").path("204").has("content")).isFalse();
            var schemas = doc.path("components").path("schemas"); var session = schemas.path("EdgeSessionResult");
            assertThat(session.path("properties").size()).isEqualTo(4); assertThat(session.path("required")).hasSize(4);
            assertThat(session.path("properties").path("displayName").path("maxLength").asInt()).isEqualTo(256);
            assertThat(schemas.path("EdgeLogoutFailureResult").path("required")).hasSize(4);
            assertThat(schemas.path("EdgeLogoutFailureResult").path("properties").path("remoteState").path("enum")).hasSize(2);
        }
    }

    @lombok.extern.slf4j.Slf4j
    @Configuration(value = "edgeDocumentFixtureConfiguration", proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
    @org.springframework.web.reactive.config.EnableWebFlux
    static class EdgeDocumentFixtureConfiguration {
        @org.springframework.context.annotation.Bean("edgeDocumentFixtureChain")
        SecurityWebFilterChain documentChain(ServerHttpSecurity http) { return http.csrf(ServerHttpSecurity.CsrfSpec::disable).authorizeExchange(authorize -> authorize.anyExchange().permitAll()).build(); }
        @org.springframework.context.annotation.Bean("edgeBffFixtureGroup")
        org.springdoc.core.models.GroupedOpenApi group() { return org.springdoc.core.models.GroupedOpenApi.builder().group("edge-bff")
                .pathsToMatch("/bff/**", "/oauth2/authorization/**", "/login/oauth2/code/**").build(); }
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

    @Test
    void configurationRequiresAnExplicitResourceCatalog() {
        EdgeOAuthProperties properties = propertiesWithIssuerOrigin();
        properties.getSession().getEncryption().setKeyId("session-key-v1");
        properties.getSession().getEncryption().setKeyFile("/mounted/session-key");
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("resource");
    }

    @Test
    void actualBootBinderPreservesExactBracketedIssuerUrlKeys() {
        String issuer = "https://idp.example/tenants/1001";
        var source = new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(Map.of(
                "egon.yuheng.edge.resources.edge-ui.resource-uri", "https://edge.example/resource",
                "egon.yuheng.edge.resources.edge-ui.default-registration-id", "edge-ui-a",
                "egon.yuheng.edge.resources.edge-ui.issuer-registration-ids.[" + issuer + "]", "edge-ui-a",
                "egon.yuheng.edge.resources.edge-ui.return-path", "/"));
        var bound = new org.springframework.boot.context.properties.bind.Binder(source)
                .bind("egon.yuheng.edge", org.springframework.boot.context.properties.bind.Bindable.of(EdgeOAuthProperties.class)).get();
        assertThat(bound.getResources().get("edge-ui").getIssuerRegistrationIds()).containsExactlyEntriesOf(Map.of(issuer, "edge-ui-a"));
    }

    @Test
    void lifecycleSchemaStateAndAbsoluteDeadlineAreValidatedWithoutPublicIdentityFields() throws Exception {
        var binding = top.egon.cola.component.yuheng.edge.service.domain.bo.EdgeSessionBindingBO.builder()
                .bindingId(java.util.UUID.randomUUID().toString()).generation(1).fencingToken(1)
                .originalCreatedAt(NOW).absoluteExpiresAt(NOW.plusSeconds(3600)).build();
        try (var validator = new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet(); assertThat(validator.validate(binding)).isEmpty();
            binding.setSchemaVersion(1); assertThat(validator.validate(binding)).isNotEmpty(); binding.setSchemaVersion(2);
            binding.setState(top.egon.cola.component.yuheng.edge.service.domain.enums.EdgeSessionBindingStateEnum.ACTIVE);
            assertThat(validator.validate(binding)).isNotEmpty();
            binding.setIssuer(URI.create("https://idp.example/tenants/1001")).setTenantId("1001").setSubject("user-1");
            assertThat(validator.validate(binding)).isEmpty();
            binding.setState(top.egon.cola.component.yuheng.edge.service.domain.enums.EdgeSessionBindingStateEnum.CLOSED);
            assertThat(validator.validate(binding)).isNotEmpty(); binding.setClosedAt(NOW);
            assertThat(validator.validate(binding)).isEmpty();
        }
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        String serialized = mapper.writeValueAsString(binding);
        assertThat(serialized).contains("\"state\":\"CLOSED\"").doesNotContain("stateValid");
        assertThat(mapper.readValue(serialized, top.egon.cola.component.yuheng.edge.service.domain.bo.EdgeSessionBindingBO.class)).isEqualTo(binding);
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
        for (int index = 1; properties.containsKey(GatewayProperties.PREFIX + ".routes[" + index + "].id"); index++) {
            String prefix = GatewayProperties.PREFIX + ".routes[" + index + "]";
            String alias = properties.getProperty(prefix + ".metadata.edge-registration-id");
            assertThat(alias).isNotBlank();
            assertThat(properties.getProperty(prefix + ".filters[0]")).isEqualTo("EdgeResourceAuthorization=" + alias);
            assertThat(properties.getProperty("egon.yuheng.edge.resources." + alias + ".resource-server-id")).isNotBlank();
        }
        assertThat(properties.getProperty("spring.cloud.gateway.routes[0].id")).isNull();
    }

    private Properties loadYaml(Path yamlPath) throws Exception {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ByteArrayResource(Files.readAllBytes(yamlPath)));
        Properties properties = yaml.getObject();
        return properties;
    }
}
