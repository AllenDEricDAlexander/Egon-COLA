package top.egon.cola.component.yuheng.llm.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import reactor.core.publisher.Flux;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayConfiguration;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.config.LlmSecurityConfiguration;
import top.egon.cola.component.yuheng.llm.proxy.controller.LlmApiController;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;
import top.egon.cola.component.yuheng.llm.proxy.repository.LlmConfigurationRepository;
import top.egon.cola.component.yuheng.llm.proxy.service.AnthropicMessagesProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmInvocationService;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmRouteSelectionStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmServletStreamComponent;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiChatProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiEmbeddingProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiResponsesProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.impl.LlmInvocationServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpSecurityObservationAutoConfiguration;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedTenantContextFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

/** Real local RS256, strict producer and Spring FilterChain; only infrastructure and upstream model I/O are controlled. */
class LlmSecurityConfigurationTest {
    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");
    private static final URI ISSUER = URI.create("https://idp.example/tenants/1001");
    private static final URI RESOURCE = URI.create("https://llm.example/resource");
    @TempDir Path temporary;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final RedissonClient redis = mock(RedissonClient.class);
    @SuppressWarnings("unchecked") private final RStream<String, String> stream = mock(RStream.class);
    private final LlmConfigurationRepository repository = mock(LlmConfigurationRepository.class);
    private final java.util.List<Map<String, String>> events = new java.util.ArrayList<>();
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private RSAKey key;
    private LlmInvocationService service;
    private OpenAiChatProtocolStrategy chat;
    private LlmModelSnapshotBO model;

    @BeforeEach
    void prepare() throws Exception {
        validator.afterPropertiesSet();
        Path keyFile = temporary.resolve("audit.key");
        Files.writeString(keyFile, "0123456789abcdef0123456789abcdef");
        Files.setPosixFilePermissions(keyFile, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        doReturn(stream).when(redis).getStream(org.mockito.ArgumentMatchers.eq("egon:oauth:audit:v1"),
                org.mockito.ArgumentMatchers.eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenAnswer(call -> {
            org.redisson.api.stream.StreamAddParams<String, String> fields = call.getArgument(0);
            events.add(Map.copyOf(fields.getEntries())); return new StreamMessageId(events.size(), 1);
        });
        var audit = new OAuthAuditProperties("egon:oauth:audit:v1", temporary.resolve("spool"), 1_048_576, keyFile);
        var usage = new OAuthUsageObservationProperties(true, ISSUER, false, IdpSecurityObservationReceiverEnum.LLM);
        var producer = new TokenUsageAuditComponent(redis, mapper, audit, new AuditDeliveryProperties(), Clock.fixed(NOW, ZoneOffset.UTC), validator);
        var converter = (TokenUsageAuditEventConverter) Class.forName(TokenUsageAuditEventConverter.class.getName() + "Impl").getConstructor().newInstance();
        var observation = new IdpObservedVerificationComponent(producer, converter, usage, sample -> { }, Clock.fixed(NOW, ZoneOffset.UTC), validator, RESOURCE);
        var properties = new IdpStarterProperties(); properties.setResourceServerId("llm"); properties.setResourceUri(RESOURCE);
        properties.getRevocation().setPrimaryEndpoint("rediss://redis.example:6379"); properties.getRevocation().setEpoch("epoch-1");
        var trusted = new IdpStarterProperties.TrustedIssuerSettings();
        trusted.setIssuer(ISSUER.toASCIIString()); trusted.setJwksUri(ISSUER + "/.well-known/jwks.json");
        trusted.setRealmKey("tenant:1001"); trusted.setRealmType("TENANT"); trusted.setTenantId("1001"); trusted.setStatus("ACTIVE"); trusted.setTrustVersion(1);
        properties.setTrustedIssuers(Map.of("tenant-1001", trusted));
        var managers = new TenantIssuerAuthenticationManagerComponent(new TrustedIssuerRegistry(properties), properties, Clock.fixed(NOW, ZoneOffset.UTC));
        managers.initialize();
        var rest = TenantIssuerAuthenticationManagerComponent.class.getDeclaredField("jwksRestOperations"); rest.setAccessible(true);
        var jwks = MockRestServiceServer.bindTo((RestTemplate) rest.get(managers)).build();
        key = new RSAKeyGenerator(2048).keyID("key-1").algorithm(JWSAlgorithm.RS256).generate();
        jwks.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(ISSUER + "/.well-known/jwks.json"))
                .andRespond(MockRestResponseCreators.withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
        var revocations = mock(OAuthRevocationReader.class); when(revocations.check(any())).thenReturn(OAuthRevocationReader.Result.valid());
        var state = mock(IdentityResourceServerStateReader.class);
        when(state.read("llm")).thenReturn(Optional.of(new IdentityResourceServerState("llm", RESOURCE, "commerce", "llm", "test", ResourceServerStatus.ACTIVE, 1)));
        var principalConverter = (VerifiedPrincipalConverter) Class.forName(VerifiedPrincipalConverter.class.getName() + "Impl").getConstructor().newInstance();
        var verifier = IdpJwtVerifier.createConfigured(null, state, mock(IdentityOAuthClientStateReader.class), "llm", RESOURCE, "platform",
                Clock.fixed(NOW, ZoneOffset.UTC), managers, revocations, principalConverter, null, observation);
        var llm = new LlmGatewayProperties().setEnabled(true).setAllowedLocalCidrs(List.of("local-llm.example"))
                .setAllowedCloudHosts(List.of()).setSecretsRoot("/run/secrets/test-llm").setMaxRequestBytes(1_048_576L)
                .setMaxFrameBytes(65_536).setMaximumAttempts(1).setStreamingThreads(1)
                .setIdentity(new LlmGatewayProperties.Identity().setResourceUri(RESOURCE.toASCIIString()).setServiceTokenAudience("llm"));
        chat = spy(new OpenAiChatProtocolStrategy(llm, mapper));
        doAnswer(call -> ((LlmInvocationResultVO) call.getArgument(2)).setBody(Flux.just(DefaultDataBufferFactory.sharedInstance
                .wrap("{\"id\":\"fixture\",\"model\":\"company-chat\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}]}".getBytes(StandardCharsets.UTF_8)))))
                .when(chat).exchange(any(), any(), any());
        var wiring = new LlmGatewayConfiguration(llm, mapper);
        Map<LlmProtocolEnum, String> names = wiring.llmProtocolStrategyRegistry();
        Map<String, LlmProtocolStrategy> strategies = Map.of(names.get(LlmProtocolEnum.OPENAI_CHAT), chat,
                names.get(LlmProtocolEnum.OPENAI_EMBEDDING), new OpenAiEmbeddingProtocolStrategy(llm, mapper),
                names.get(LlmProtocolEnum.OPENAI_RESPONSES), new OpenAiResponsesProtocolStrategy(llm, mapper),
                names.get(LlmProtocolEnum.ANTHROPIC_MESSAGES), new AnthropicMessagesProtocolStrategy(llm, mapper));
        model = model(); when(repository.findSnapshot("company-chat")).thenReturn(Optional.of(model));
        when(repository.findCatalog()).thenReturn(List.of(model));
        service = spy(new LlmInvocationServiceImpl(repository, new LlmRouteSelectionStrategy(llm), llm));
        var writer = new LlmServletStreamComponent(llm, wiring.llmStreamingExecutor(), mapper);
        var controller = new LlmApiController(service, llm, mapper, names, strategies, writer, repository);
        controller.checkRegistry();
        context = new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.setEnvironment(new MockEnvironment().withProperty("egon.cola.platform.tianquan.shoubing.usage-observation.enabled", "true")
                .withProperty("server.ssl.enabled", "true").withProperty("TIANSHU_TENANT_ID", "1001"));
        context.addBeanFactoryPostProcessor(factory -> {
            factory.registerSingleton("defaultValidator", validator); factory.registerSingleton("jacksonObjectMapper", mapper);
            factory.registerSingleton("oauthAuditProperties", audit); factory.registerSingleton("oauthUsageObservationProperties", usage);
            factory.registerSingleton("tokenUsageAuditComponent", producer); factory.registerSingleton("idpObservedVerificationComponent", observation);
            factory.registerSingleton("idpStarterProperties", properties); factory.registerSingleton("idpJwtVerifier", verifier);
            factory.registerSingleton("idpClock", Clock.fixed(NOW, ZoneOffset.UTC)); factory.registerSingleton("llmMeters", meters);
            factory.registerSingleton("llmApiController", controller); factory.registerSingleton(LlmGatewayProperties.BEAN_NAME, llm);
            factory.registerSingleton("llmProtocolStrategyRegistry", names);
            strategies.forEach(factory::registerSingleton);
        });
        context.register(LlmSecurityConfiguration.class, IdpSecurityObservationAutoConfiguration.class, FixtureConfiguration.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(new top.egon.cola.component.yuheng.llm.config.LlmClientCredentialFilter(mapper),
                context.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)).build();
    }

    @AfterEach void cleanup() { if (context != null) { context.close(); } validator.close(); meters.close(); SecurityContextHolder.clearContext(); org.slf4j.MDC.clear(); }

    @Test
    void startupAllowsExplicitLocalLoopbackRedis() {
        var properties = context.getBean("idpStarterProperties", IdpStarterProperties.class);
        context.getBean("oauthUsageObservationProperties", OAuthUsageObservationProperties.class).setAllowLocalPlaintext(true);
        var environment = localStartupEnvironment();
        for (String endpoint : List.of("redis://127.0.0.1:6379", "redis://localhost:6379", "redis://[::1]:6379")) {
            properties.getRevocation().setPrimaryEndpoint(endpoint);
            assertThat(startupCheck(environment, true)).as(endpoint).isNotNull();
        }
    }

    @Test
    void startupLocalExceptionKeepsTheSdkRedisUriValidation() {
        var properties = context.getBean("idpStarterProperties", IdpStarterProperties.class);
        context.getBean("oauthUsageObservationProperties", OAuthUsageObservationProperties.class).setAllowLocalPlaintext(true);
        var environment = localStartupEnvironment();
        for (String endpoint : List.of("redis://redis.example:6379", "redis://127.0.0.2:6379",
                "redis://user:password@127.0.0.1:6379", "redis://127.0.0.1:6379?database=1",
                "redis://127.0.0.1:6379#fragment")) {
            properties.getRevocation().setPrimaryEndpoint(endpoint);
            assertThatThrownBy(() -> startupCheck(environment, true)).as(endpoint)
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("must be rediss or exact loopback redis and canonical");
        }
    }

    @Test
    void startupPlaintextRequiresTheFlagLocalProfileAndLocalDeployment() {
        context.getBean("idpStarterProperties", IdpStarterProperties.class).getRevocation().setPrimaryEndpoint("redis://127.0.0.1:6379");
        var usage = context.getBean("oauthUsageObservationProperties", OAuthUsageObservationProperties.class);
        var environment = localStartupEnvironment();
        assertThatThrownBy(() -> startupCheck(environment, true)).isInstanceOf(IllegalStateException.class)
                .hasMessage("LLM_SECURITY_TRANSPORT_REQUIRED");
        usage.setAllowLocalPlaintext(true);
        environment.setActiveProfiles("production");
        assertThatThrownBy(() -> startupCheck(environment, true)).isInstanceOf(IllegalStateException.class)
                .hasMessage("LLM_SECURITY_TRANSPORT_REQUIRED");
        environment.setActiveProfiles("local");
        environment.setProperty("DEPLOYMENT_ENV", "production");
        assertThatThrownBy(() -> startupCheck(environment, true)).isInstanceOf(IllegalStateException.class)
                .hasMessage("LLM_SECURITY_TRANSPORT_REQUIRED");
        var missingDeployment = new MockEnvironment().withProperty("server.ssl.enabled", "true");
        missingDeployment.setActiveProfiles("local");
        assertThatThrownBy(() -> startupCheck(missingDeployment, true)).isInstanceOf(IllegalStateException.class)
                .hasMessage("LLM_SECURITY_TRANSPORT_REQUIRED");
    }

    @Test
    void startupAcceptsProductionRedisTlsWithoutTheLocalException() {
        var environment = new MockEnvironment().withProperty("server.ssl.enabled", "true").withProperty("DEPLOYMENT_ENV", "production");
        environment.setActiveProfiles("production");
        assertThat(startupCheck(environment, true)).isNotNull();
    }

    @Test
    void startupLocalRedisKeepsAuditAndServerTlsRequired() {
        context.getBean("idpStarterProperties", IdpStarterProperties.class).getRevocation().setPrimaryEndpoint("redis://127.0.0.1:6379");
        var usage = context.getBean("oauthUsageObservationProperties", OAuthUsageObservationProperties.class).setAllowLocalPlaintext(true);
        var environment = localStartupEnvironment();
        assertThatThrownBy(() -> startupCheck(environment, false)).isInstanceOf(IllegalStateException.class)
                .hasMessage("LLM_SECURITY_OBSERVATION_REQUIRED");
        environment.setProperty("server.ssl.enabled", "false");
        assertThatThrownBy(() -> startupCheck(environment, true)).isInstanceOf(IllegalStateException.class)
                .hasMessage("LLM_SECURITY_OBSERVATION_REQUIRED");
        environment.setProperty("server.ssl.enabled", "true");
        usage.setEnabled(false);
        assertThatThrownBy(() -> startupCheck(environment, true)).isInstanceOf(IllegalStateException.class)
                .hasMessage("LLM_SECURITY_OBSERVATION_REQUIRED");
    }

    private MockEnvironment localStartupEnvironment() {
        var environment = new MockEnvironment().withProperty("server.ssl.enabled", "true").withProperty("DEPLOYMENT_ENV", "local");
        environment.setActiveProfiles("local");
        return environment;
    }

    private Object startupCheck(MockEnvironment environment, boolean auditReady) {
        return ReflectionTestUtils.invokeMethod(new LlmSecurityConfiguration(), "llmSecurityObservationStartupCheck",
                context.getBean("idpObservedVerificationComponent", IdpObservedVerificationComponent.class),
                context.getBean("oauthUsageObservationProperties", OAuthUsageObservationProperties.class),
                (java.util.function.BooleanSupplier) () -> auditReady, validator,
                context.getBean("idpStarterProperties", IdpStarterProperties.class), environment);
    }

    @Test
    void validIdentityMissingInvokeScopeHasNoInvocationOrModelCall() throws Exception {
        var response = mvc.perform(post("/v1/chat/completions").secure(true).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token("yuheng:llm:read", false)).content(body())).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(mapper.readTree(response.getContentAsString()).path("error").path("param").isNull()).isTrue();
        verify(service, never()).invoke(any()); verify(chat, never()).exchange(any(), any(), any());
        assertThat(events).hasSize(2); assertThat(events.getLast()).containsEntry("reasonCode", "AUTHORIZATION_DENIED");
    }

    @Test
    void validScopeStillNeedsRealAllowedSubjectsAndCapabilities() throws Exception {
        model.setAllowedSubjects(List.of("another-subject"));
        var denied = mvc.perform(post("/v1/chat/completions").secure(true).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token("yuheng:llm:invoke", false)).content(body())).andReturn().getResponse();
        assertThat(denied.getStatus()).isEqualTo(403);
        verify(chat, never()).exchange(any(), any(), any());
    }

    @Test
    void validScopeCannotBypassTheRealModelCapabilityRule() throws Exception {
        String tools = "{\"model\":\"company-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}],"
                + "\"tools\":[{\"type\":\"function\",\"function\":{\"name\":\"lookup\",\"parameters\":{\"type\":\"object\"}}}]}";
        var denied = mvc.perform(post("/v1/chat/completions").secure(true).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token("yuheng:llm:invoke", false)).content(tools)).andReturn().getResponse();
        assertThat(denied.getStatus()).isEqualTo(503);
        verify(chat, never()).exchange(any(), any(), any());
    }

    @Test
    void messagesCarrierUsesTheSameScopeGateAndNativeAnthropicError() throws Exception {
        var response = mvc.perform(post("/v1/messages").secure(true).contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", token("yuheng:llm:read", true)).content(body())).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(403);
        var error = mapper.readTree(response.getContentAsString());
        assertThat(error.path("type").asText()).isEqualTo("error");
        assertThat(error.path("error").has("param")).isFalse(); assertThat(error.path("error").has("code")).isFalse();
        verify(service, never()).invoke(any());
    }

    @Test
    void filtersAreOnlyInstalledInTheSecurityChainAndAllProfilesBindTheSameSecurityLeaves() throws Exception {
        assertThat(context.getBean("llmVerifiedPrincipalFilterRegistration", org.springframework.boot.web.servlet.FilterRegistrationBean.class).isEnabled()).isFalse();
        assertThat(context.getBean("llmProtocolSecurityFilterRegistration", org.springframework.boot.web.servlet.FilterRegistrationBean.class).isEnabled()).isFalse();
        Path parent = Path.of("").toAbsolutePath().getParent();
        for (String owner : List.of("llm", "biz", "mcp")) {
            Path resources = parent.resolve("yuheng-" + owner + "-gateway/src/main/resources");
            java.util.Set<String> first = null;
            for (String name : List.of("application.yml", owner.equals("llm") ? "application-local.yml" : "application-operations.yml")) {
                var sources = new org.springframework.boot.env.YamlPropertySourceLoader().load(name,
                        new org.springframework.core.io.FileSystemResource(resources.resolve(name)));
                var names = new java.util.TreeSet<String>();
                var environment = new MockEnvironment().withProperty("TIANQUAN_SHOUBING_OAUTH_ISSUER", ISSUER.toASCIIString());
                for (var source : sources) {
                    environment.getPropertySources().addLast(source);
                    for (String key : ((org.springframework.core.env.EnumerablePropertySource<?>) source).getPropertyNames()) {
                        if (key.startsWith("egon.cola.platform.tianquan.shoubing.")) { names.add(key); }
                    }
                }
                if (first == null) { first = names; } else { assertThat(names).as(owner + "/" + name).isEqualTo(first); }
                var usage = org.springframework.boot.context.properties.bind.Binder.get(environment)
                        .bind("egon.cola.platform.tianquan.shoubing.usage-observation", OAuthUsageObservationProperties.class).get();
                assertThat(validator.validate(usage)).isEmpty();
                assertThat(usage.isEnabled()).isTrue();
                assertThat(usage.getReceiver().name()).isEqualTo(owner.toUpperCase(java.util.Locale.ROOT));
                if (owner.equals("mcp")) {
                    assertThat(environment.getProperty("egon.cola.component.yuheng.mcp-engine.listener.tls.enabled", Boolean.class)).isTrue();
                    assertThat(environment.getProperty("egon.cola.component.yuheng.mcp-engine.outbound.rpc-tls.enabled", Boolean.class)).isTrue();
                    assertThat(environment.getProperty("egon.cola.component.yuheng.mcp-engine.listener.tls.development-plaintext", Boolean.class)).isFalse();
                } else if (owner.equals("biz")) {
                    assertThat(environment.getProperty("egon.cola.component.yuheng.engine.http.public-tls.enabled", Boolean.class)).isTrue();
                    assertThat(environment.getProperty("egon.cola.component.yuheng.engine.http.internal-tls.enabled", Boolean.class)).isTrue();
                } else { assertThat(environment.getProperty("server.ssl.enabled", Boolean.class)).isTrue(); }
            }
        }
    }

    @Test
    void userAndServiceWithScopeAndRealFineAccessKeepTheOriginalSuccessShape() throws Exception {
        for (boolean serviceToken : List.of(false, true)) {
            var response = mvc.perform(post("/v1/chat/completions").secure(true).contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token("yuheng:llm:invoke", serviceToken)).content(body())).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(mapper.readTree(response.getContentAsString()).path("choices").get(0).path("message").path("content").asText()).isEqualTo("ok");
            assertThat(response.getContentAsString()).doesNotContain("tokenRef", "proof", "secretRef", "data\":{");
        }
    }

    @Test
    void missingCredentialAndAuditOutageUseNativeErrorsAndNeverInvokeTheModel() throws Exception {
        var missing = mvc.perform(get("/v1/models").secure(true)).andReturn().getResponse();
        assertThat(missing.getStatus()).isEqualTo(401);
        assertThat(mapper.readTree(missing.getContentAsString()).has("error")).isTrue();
        when(stream.add(any(StreamAddArgs.class))).thenThrow(new org.redisson.client.RedisException("controlled audit outage"));
        var unavailable = mvc.perform(post("/v1/chat/completions").secure(true).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token("yuheng:llm:invoke", false)).content(body())).andReturn().getResponse();
        assertThat(unavailable.getStatus()).isEqualTo(503);
        assertThat(unavailable.getContentAsString()).contains("error").doesNotContain("controlled audit outage", "OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
        verify(service, never()).invoke(any()); verify(chat, never()).exchange(any(), any(), any());
    }

    private String token(String scope, boolean serviceToken) throws Exception {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER.toASCIIString()); claims.put("sub", serviceToken ? "llm-service" : "operator");
        claims.put("aud", List.of(RESOURCE.toASCIIString())); claims.put("iat", NOW.minusSeconds(1).getEpochSecond()); claims.put("nbf", NOW.minusSeconds(1).getEpochSecond());
        claims.put("exp", NOW.plusSeconds(300).getEpochSecond()); claims.put("jti", serviceToken ? "902" : "901");
        claims.put("tenant_id", "1001"); claims.put("client_id", serviceToken ? "llm-service" : "browser-client");
        claims.put("principal_type", serviceToken ? "SERVICE" : "USER"); claims.put("scope_context", "TENANT"); claims.put("scope", scope);
        claims.put("authorization_id", "801"); claims.put("client_registration_id", "501");
        claims.put("security_version", serviceToken ? Map.of("realm", 1L, "client", 2L) : Map.of("realm", 1L, "client", 2L, "subject", 3L));
        if (serviceToken) {
            claims.put("app_id", "llm"); claims.put("resource_version", 1L); claims.put("credential_id", "501:0");
            claims.put("source_biz", "commerce"); claims.put("source_app", "llm"); claims.put("source_env", "test");
        } else { claims.put("acr", "PASSWORD"); claims.put("auth_time", NOW.minusSeconds(5).getEpochSecond()); }
        var signed = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(new JOSEObjectType("at+jwt")).keyID("key-1").build(), JWTClaimsSet.parse(claims));
        signed.sign(new RSASSASigner(key)); return signed.serialize();
    }

    private String body() { return "{\"model\":\"company-chat\",\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}],\"max_tokens\":8}"; }

    private LlmModelSnapshotBO model() {
        var channel = new LlmModelSnapshotBO.ChannelBO().setChannelKey("local-a").setEnabled(true).setDeployment(LlmDeploymentEnum.LOCAL)
                .setProtocol(LlmProtocolEnum.OPENAI_CHAT).setBaseUrl("https://local-llm.example/v1").setSecretRef("env:LLM_TEST_SECRET")
                .setConnectTimeoutMs(1000).setHeaderTimeoutMs(1000).setIdleTimeoutMs(1000).setTotalTimeoutMs(3000).setMaxConcurrent(1);
        var route = new LlmModelSnapshotBO.RouteBO().setChannelKey("local-a").setUpstreamModel("fixture-model").setPriority(1).setWeight(1)
                .setCapabilities(EnumSet.of(LlmCapabilityEnum.TEXT)).setChannel(channel);
        return new LlmModelSnapshotBO().setModelKey("company-chat").setName("Fixture model").setKind(LlmModelKindEnum.CHAT).setCreatedAt(NOW)
                .setEnabled(true).setProtocols(List.of(LlmProtocolEnum.OPENAI_CHAT)).setAllowedSubjects(List.of("operator", "llm-service"))
                .setRevision(1L).setRoutes(List.of(route));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    static class FixtureConfiguration {
        @Bean(name = "idpEndpointAuthenticationPolicy")
        @ConditionalOnMissingBean(IdpEndpointAuthenticationPolicy.class)
        IdpEndpointAuthenticationPolicy policy() { return new IdpEndpointAuthenticationPolicy(List.of(), List.of(), List.of("/v1/**"), false); }
        @Bean(name = "idpBearerAuthenticationFilter")
        IdpBearerAuthenticationFilter bearer(IdpJwtVerifier verifier, ObjectMapper mapper, IdpEndpointAuthenticationPolicy policy) {
            return IdpBearerAuthenticationFilter.create(new UserAccessTokenVerifier(verifier), new ServiceAccessTokenVerifier(verifier),
                    policy, mapper, new VerifiedTenantContextFilter(), verifier);
        }
        @Bean(name = "idpSecurityObservationFilter")
        IdpSecurityObservationFilter observation(IdpObservedVerificationComponent observations, IdpEndpointAuthenticationPolicy policy) {
            return new IdpSecurityObservationFilter(observations, policy);
        }
        /** Baseline identity-only chain: it verifies tokens but has no LLM authorization gate. */
        @Bean(name = "baselineIdentityOnlyChain")
        @Order(1000)
        SecurityFilterChain baseline(HttpSecurity http, IdpBearerAuthenticationFilter bearer,
                @org.springframework.beans.factory.annotation.Qualifier("llmVerifiedPrincipalRequestFilter") org.springframework.web.filter.OncePerRequestFilter projection) throws Exception {
            http.csrf(csrf -> csrf.disable()).addFilterBefore(bearer, AnonymousAuthenticationFilter.class)
                    .addFilterAfter(projection, org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return http.build();
        }
    }
}
