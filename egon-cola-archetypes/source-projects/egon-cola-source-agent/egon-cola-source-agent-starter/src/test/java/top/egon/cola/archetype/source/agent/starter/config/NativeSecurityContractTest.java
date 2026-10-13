package top.egon.cola.archetype.source.agent.starter.config;

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
import jakarta.validation.Validator;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamAddParams;
import org.redisson.client.codec.StringCodec;
import org.slf4j.MDC;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.Decision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.tianquan.jianshen.core.decision.FieldPolicyMerger;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.Rbac3ObservedAuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterAutoConfiguration;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.AuthorizationSnapshotCache;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient;
import top.egon.cola.platform.tianquan.jianshen.starter.field.FieldWriteAuthorizationConverter;
import top.egon.cola.platform.tianquan.jianshen.starter.field.Rbac3FieldWriteAuthorizationComponent;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3BearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3ContextAuthentication;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
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
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.jianshen.starter.web.Rbac3AuthorizationExceptionHandler;
import org.junit.jupiter.api.io.TempDir;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.filter.KnowledgeTenantMdcFilter;
import top.egon.cola.archetype.source.agent.adapter.knowledge.controller.KnowledgeBaseController;
import top.egon.cola.archetype.source.agent.adapter.knowledge.controller.KnowledgeDocumentController;
import top.egon.cola.archetype.source.agent.adapter.knowledge.controller.KnowledgeQaController;
import top.egon.cola.archetype.source.agent.adapter.research.controller.DeepResearchController;
import top.egon.cola.archetype.source.agent.adapter.knowledge.handler.KnowledgeGlobalExceptionHandler;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchGlobalExceptionHandler;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeBaseManage;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeDocumentManage;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeQaManage;
import top.egon.cola.archetype.source.agent.application.research.manage.DeepResearchManage;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.impl.KnowledgeBaseManageImpl;
import top.egon.cola.archetype.source.agent.application.knowledge.config.KnowledgeRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.research.config.DeepResearchRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.knowledge.service.KnowledgeRemovalService;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeBaseRepository;
import top.egon.cola.archetype.source.agent.domain.knowledge.repository.KnowledgeDocumentRepository;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Actual owned host, signed SDK verifier/Core PDP and original application query; only remote/data ports are controlled. */
public class NativeSecurityContractTest {
    @Test
    void legacyApiKeyCannotAuthorizeTheActualKnowledgeRead(@TempDir Path temporary) throws Exception {
        var bases = mock(KnowledgeBaseRepository.class);
        try (var security = new SecurityFixture(temporary)) {
            hostRunner(bases, security).run(context -> {
                assertThat(context).hasNotFailed();
                mvc(context, security).perform(get("/api/v1/knowledge-bases").header("X-Research-Api-Key", "legacy-test-key"))
                        .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
                verifyNoInteractions(bases);
            });
        }
    }

    @Test
    void everyHttpAndSseEntryRequiresUserConsentBeforeBusiness(@TempDir Path temporary) throws Exception {
        var bases = mock(KnowledgeBaseRepository.class);
        try (var security = new SecurityFixture(temporary)) {
            hostRunner(bases, security).run(context -> {
                assertThat(context).hasNotFailed();
                var mvc = mvc(context, security);
                for (int entry = 0; entry < 14; entry++) {
                    mvc.perform(request(entry)).andExpect(status().isUnauthorized());
                    mvc.perform(request(entry).header("Authorization", "Bearer " + security.serviceToken("1001", Set.copyOf(security.permissions))))
                            .andExpect(status().isUnauthorized());
                    mvc.perform(request(entry).header("Authorization", "Bearer " + security.token("1001", Set.of("openid"))))
                            .andExpect(status().isForbidden()).andExpect(header().string("Cache-Control", "no-store"));
                }
                assertThat(security.permissionChecks).isZero();
                verifyNoInteractions(bases, context.getBean("knowledgeDocumentManage"), context.getBean("knowledgeQaManage"), context.getBean("deepResearchManage"));
            });
        }
    }

    @Test
    void signedReadUsesTrustedTenantAndActorAndRestoresTheCaller(@TempDir Path temporary) throws Exception {
        var bases = mock(KnowledgeBaseRepository.class);
        when(bases.page(0, 20, null, null)).thenAnswer(call -> {
            assertThat(MDC.get("tenantId")).isEqualTo("1002");
            assertThat(MDC.get("userId")).isEqualTo("subject-1002");
            assertThat(MDC.get("egon.auth.actor-kind")).isEqualTo("USER");
            return List.of();
        });
        try (var security = new SecurityFixture(temporary)) {
            hostRunner(bases, security).run(context -> {
                assertThat(context).hasNotFailed();
                var mvc = mvc(context, security);
                mvc.perform(get("/api/v1/knowledge-bases").header("Authorization", "Bearer " + security.token("1002", Set.of(SecurityFixture.SYSTEM + ":knowledge-base:list")))
                                .header("X-Tenant-Id", "999").header("X-Actor-Id", "forged").header("X-Research-Api-Key", "legacy-test-key"))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
                assertThat(MDC.get("tenantId")).isNull(); assertThat(MDC.get("userId")).isNull();
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                security.permissions.clear(); security.clearSnapshots();
                mvc.perform(get("/api/v1/knowledge-bases").header("Authorization", "Bearer " + security.token("1001", Set.of(SecurityFixture.SYSTEM + ":knowledge-base:list"))))
                        .andExpect(status().isForbidden());
            });
        }
    }

    @Test
    void threeWriteSetsRequireExplicitWriteRulesBeforeTheUseCases(@TempDir Path temporary) throws Exception {
        var bases = mock(KnowledgeBaseRepository.class);
        try (var security = new SecurityFixture(temporary)) {
            hostRunner(bases, security).run(context -> {
                assertThat(context).hasNotFailed(); var mvc = mvc(context, security);
                for (FieldAccessLevel level : List.of(FieldAccessLevel.READ, FieldAccessLevel.NONE)) {
                    security.fieldLevel = level; security.clearSnapshots();
                    for (int entry : List.of(1, 4, 6)) {
                        mvc.perform(request(entry).header("Authorization", "Bearer " + security.token("1001", Set.copyOf(security.permissions))))
                                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FIELD_ACCESS_DENIED"));
                    }
                }
                security.fieldLevel = FieldAccessLevel.WRITE; security.explicitFieldRules = false; security.clearSnapshots();
                for (int entry : List.of(1, 4, 6)) {
                    mvc.perform(request(entry).header("Authorization", "Bearer " + security.token("1001", Set.copyOf(security.permissions))))
                            .andExpect(status().isForbidden());
                }
                verifyNoInteractions(bases, context.getBean("knowledgeDocumentManage"));
            });
        }
    }

    @Test
    void securityStateAndStrictAuditFailureCannotEnterBusiness(@TempDir Path temporary) throws Exception {
        var bases = mock(KnowledgeBaseRepository.class);
        try (var security = new SecurityFixture(temporary)) {
            hostRunner(bases, security).run(context -> {
                assertThat(context).hasNotFailed(); var mvc = mvc(context, security);
                security.snapshotAvailable = false;
                mvc.perform(get("/api/v1/knowledge-bases").header("Authorization", "Bearer " + security.token("1001", Set.copyOf(security.permissions))))
                        .andExpect(status().isServiceUnavailable());
                security.snapshotAvailable = true; security.clearSnapshots();
                Mockito.when(security.auditStream.add(ArgumentMatchers.any(StreamAddArgs.class))).thenThrow(new org.redisson.client.RedisException("controlled unavailable"));
                mvc.perform(get("/api/v1/knowledge-bases").header("Authorization", "Bearer " + security.token("1001", Set.copyOf(security.permissions))))
                        .andExpect(status().isServiceUnavailable());
                verifyNoInteractions(bases);
            });
        }
    }

    @Test
    void admittedSseCompletesThroughTheActualHostAsyncDispatch(@TempDir Path temporary) throws Exception {
        var bases = mock(KnowledgeBaseRepository.class);
        try (var security = new SecurityFixture(temporary)) {
            hostRunner(bases, security).run(context -> {
                assertThat(context).hasNotFailed();
                var manage = context.getBean("deepResearchManage", DeepResearchManage.class);
                Mockito.when(manage.startResearch(ArgumentMatchers.any(), ArgumentMatchers.any())).thenAnswer(call -> {
                    var observer = call.getArgument(1, top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchEventObserverService.class);
                    observer.onEvent(top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent.started("run-1", "trace-1", SecurityFixture.NOW));
                    observer.onEvent(top.egon.cola.archetype.source.agent.domain.research.model.DeepResearchEvent.completed("run-1", 2, "report", SecurityFixture.NOW, "trace-1"));
                    return mock(top.egon.cola.archetype.source.agent.domain.research.service.DeepResearchRunService.class);
                });
                var mvc = mvc(context, security);
                var result = mvc.perform(request(0).header("Authorization", "Bearer " + security.token("1001", Set.of(SecurityFixture.SYSTEM + ":research:start"))))
                        .andExpect(status().isOk()).andReturn();
                if (result.getRequest().isAsyncStarted()) { result = mvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn(); }
                assertThat(result.getResponse().getContentAsString()).contains("research.started", "research.completed");
                assertThat(MDC.get("tenantId")).isNull(); assertThat(MDC.get("userId")).isNull();
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                verifyNoInteractions(bases);
            });
        }
    }

    private static MockHttpServletRequestBuilder request(int entry) {
        return switch (entry) {
            case 0 -> post("/api/v1/deep-research/runs").accept("text/event-stream").contentType("application/json").content("{\"topic\":\"agent architecture\"}");
            case 1 -> post("/api/v1/knowledge-bases").contentType("application/json").content("{\"code\":\"docs\",\"name\":\"Docs\",\"embeddingModel\":\"model\",\"chunkStrategy\":\"TOKEN\",\"chunkConfig\":{\"maxTokensPerChunk\":512,\"overlapTokens\":64,\"minChunkChars\":1}}");
            case 2 -> get("/api/v1/knowledge-bases");
            case 3 -> get("/api/v1/knowledge-bases/1001");
            case 4 -> put("/api/v1/knowledge-bases/1001").contentType("application/json").content("{\"name\":\"Docs\"}");
            case 5 -> delete("/api/v1/knowledge-bases/1001");
            case 6 -> multipart("/api/v1/knowledge-bases/1001/documents").file(new MockMultipartFile("file", "notes.txt", "text/plain", "notes".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            case 7 -> get("/api/v1/knowledge-bases/1001/documents");
            case 8 -> get("/api/v1/knowledge-documents/1001");
            case 9 -> post("/api/v1/knowledge-documents/1001/reingest");
            case 10 -> delete("/api/v1/knowledge-documents/1001");
            case 11 -> post("/api/v1/knowledge-bases/1001/retrieve").contentType("application/json").content("{\"query\":\"hello\"}");
            case 12 -> post("/api/v1/knowledge-bases/1001/chat").accept("text/event-stream").contentType("application/json").content("{\"question\":\"hello\"}");
            case 13 -> post("/api/v1/knowledge-bases/1001/chat").accept("application/json").contentType("application/json").content("{\"question\":\"hello\"}");
            default -> throw new IllegalArgumentException("unknown entry");
        };
    }

    private static org.springframework.test.web.servlet.MockMvc mvc(org.springframework.web.context.WebApplicationContext context, SecurityFixture security) {
        return MockMvcBuilders.webAppContextSetup(context).addFilters(new ResearchTraceFilter(security.clock, security.mapper),
                context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    static WebApplicationContextRunner hostRunner(KnowledgeBaseRepository bases, SecurityFixture security) {
        var manage = new KnowledgeBaseManageImpl(bases, mock(KnowledgeDocumentRepository.class), mock(KnowledgeRemovalService.class),
                new KnowledgeRuntimeProperties(null, null), new ValidationUtils(security.validator));
        var documents = mock(KnowledgeDocumentManage.class); var qa = mock(KnowledgeQaManage.class); var research = mock(DeepResearchManage.class);
        return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
                        HttpMessageConvertersAutoConfiguration.class, ValidationAutoConfiguration.class, WebMvcAutoConfiguration.class,
                        SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class))
                .withUserConfiguration(NativeHttpSecurityConfiguration.class, KnowledgeGlobalExceptionHandler.class, DeepResearchGlobalExceptionHandler.class)
                .withBean("agentClock", Clock.class, () -> security.clock)
                .withBean("knowledgeBaseController", KnowledgeBaseController.class, () -> new KnowledgeBaseController(security.fields, security.properties, manage))
                .withBean("knowledgeDocumentManage", KnowledgeDocumentManage.class, () -> documents)
                .withBean("knowledgeDocumentController", KnowledgeDocumentController.class, () -> new KnowledgeDocumentController(security.fields, security.properties, documents))
                .withBean("knowledgeQaManage", KnowledgeQaManage.class, () -> qa)
                .withBean("knowledgeQaController", KnowledgeQaController.class, () -> new KnowledgeQaController(qa, new KnowledgeRuntimeProperties(null, null)))
                .withBean("deepResearchManage", DeepResearchManage.class, () -> research)
                .withBean("deepResearchController", DeepResearchController.class, () -> new DeepResearchController(research, new DeepResearchRuntimeProperties(4, Duration.ofMinutes(2), 8, Duration.ofSeconds(15)), security.clock))
                .withBean("knowledgeTenantMdcFilter", KnowledgeTenantMdcFilter.class, () -> new KnowledgeTenantMdcFilter(security.mapper, "tenantId", "userId"))
                .withBean("idpBearerAuthenticationFilter", IdpBearerAuthenticationFilter.class, () -> security.identityFilter)
                .withBean("idpSecurityObservationFilter", IdpSecurityObservationFilter.class, () -> security.observationFilter)
                .withBean("rbac3BearerAuthenticationFilter", Rbac3BearerAuthenticationFilter.class, () -> security.rbacFilter)
                .withBean("rbac3AuthorizationExceptionHandler", Rbac3AuthorizationExceptionHandler.class, Rbac3AuthorizationExceptionHandler::new)
                .withBean("rbac3MethodAuthorizationInterceptor", AuthorizationManagerBeforeMethodInterceptor.class, () -> security.advisor,
                        definition -> definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE));
    }

    /** Shared explicit test composition; production auto-configuration remains disabled in the external-free test profile. */
    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    public static class SecurityTestBeans {
        @Bean(name = "nativeSecurityFixture", destroyMethod = "close")
        SecurityFixture fixture() throws Exception { return new SecurityFixture(Files.createTempDirectory("agent-security-test-")); }
        @Bean("idpJwtVerifier") IdpJwtVerifier verifier(SecurityFixture fixture) { return fixture.verifier; }
        @Bean("idpStarterProperties") IdpStarterProperties idpProperties(SecurityFixture fixture) { return fixture.identityProperties; }
        @Bean("userAccessTokenVerifier") UserAccessTokenVerifier users(SecurityFixture fixture) { return new UserAccessTokenVerifier(fixture.verifier); }
        @Bean("serviceAccessTokenVerifier") ServiceAccessTokenVerifier services(SecurityFixture fixture) { return new ServiceAccessTokenVerifier(fixture.verifier); }
        @Bean("idpObservedVerificationComponent") IdpObservedVerificationComponent observer(SecurityFixture fixture) { return fixture.observations; }
        @Bean("idpBearerAuthenticationFilter") IdpBearerAuthenticationFilter identity(SecurityFixture fixture) { return fixture.identityFilter; }
        @Bean("rbac3BearerAuthenticationFilter") Rbac3BearerAuthenticationFilter rbac(SecurityFixture fixture) { return fixture.rbacFilter; }
        @Bean("idpSecurityObservationFilter") IdpSecurityObservationFilter observations(SecurityFixture fixture) { return fixture.observationFilter; }
        @Bean("rbac3FieldWriteAuthorizationComponent") Rbac3FieldWriteAuthorizationComponent fields(SecurityFixture fixture) { return fixture.fields; }
        @Bean("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
        Rbac3StarterProperties properties(SecurityFixture fixture) { return fixture.properties; }
        @Bean("rbac3MethodAuthorizationInterceptor") @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        AuthorizationManagerBeforeMethodInterceptor advisor(SecurityFixture fixture) { return fixture.advisor; }
        @Bean("authorizationService") AuthorizationService authorization(SecurityFixture fixture) { return fixture.authorization; }
        @Bean("rbac3AuthorizationExceptionHandler") Rbac3AuthorizationExceptionHandler errors() {
            return new Rbac3AuthorizationExceptionHandler();
        }
    }

    /** Real multi-issuer verifier, audit producer, snapshot cache, PDP and field guard; only remote stores are controlled. */
    public static final class SecurityFixture implements AutoCloseable {
        public static final String SYSTEM = "egon-cola-source-agent";
        public static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");
        public static final URI RESOURCE = URI.create("https://agent.example.test");
        public final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        public final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        public final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        public final List<Map<String,String>> events = new CopyOnWriteArrayList<>();
        public final List<SecurityMetricObservationBO> metrics = new CopyOnWriteArrayList<>();
        public final RStream<String,String> auditStream = mock(RStream.class);
        public final RedissonClient redis = mock(RedissonClient.class);
        public final OAuthRevocationReader revocations = mock(OAuthRevocationReader.class);
        public final Rbac3StarterProperties properties = new Rbac3StarterProperties();
        public final IdpStarterProperties identityProperties = new IdpStarterProperties();
        public final EgonColaMybatisPlusProperties persistenceProperties = new EgonColaMybatisPlusProperties();
        public final IdpJwtVerifier verifier;
        public final IdpObservedVerificationComponent observations;
        public final IdpEndpointAuthenticationPolicy endpointPolicy = new IdpEndpointAuthenticationPolicy(List.of("/actuator/health/liveness", "/actuator/health/readiness"), List.of());
        public final IdpBearerAuthenticationFilter identityFilter;
        public final IdpSecurityObservationFilter observationFilter;
        public final Rbac3BearerAuthenticationFilter rbacFilter;
        public final AuthorizationService authorization;
        public final Rbac3FieldWriteAuthorizationComponent fields;
        public final AuthorizationManagerBeforeMethodInterceptor advisor;
        public final MockRestServiceServer jwks;
        public final RSAKey key;
        public final Set<String> permissions = new HashSet<>();
        public final Set<String> resources = new HashSet<>();
        public boolean snapshotAvailable = true;
        public FieldAccessLevel fieldLevel = FieldAccessLevel.WRITE;
        public boolean explicitFieldRules = true;
        public int permissionChecks;
        private final AtomicLong tokenIds = new AtomicLong(900);
        private final Map<AuthorizationSnapshotCache.Key,SystemAuthorizationSnapshot> stored = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public SecurityFixture(Path root) throws Exception {
            validator.afterPropertiesSet();
            var signingFile = root.resolve("audit.key"); Files.writeString(signingFile, "0123456789abcdef0123456789abcdef");
            Files.setPosixFilePermissions(signingFile, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            RStream<String,String> stream = auditStream;
            Mockito.doReturn(stream).when(redis).getStream(ArgumentMatchers.eq("egon:oauth:audit:v1"), ArgumentMatchers.eq(StringCodec.INSTANCE));
            when(stream.add(ArgumentMatchers.any(StreamAddArgs.class))).thenAnswer(call -> {
                StreamAddParams<String,String> values = call.getArgument(0); events.add(Map.copyOf(values.getEntries()));
                return new StreamMessageId(events.size(), 1);
            });
            var producer = new TokenUsageAuditComponent(redis, mapper,
                    new OAuthAuditProperties("egon:oauth:audit:v1", root.resolve("spool"), 1048576, signingFile),
                    new AuditDeliveryProperties(), clock, validator);
            var usage = new OAuthUsageObservationProperties(true, URI.create("https://auth.example.test/tenants/1001"), false);
            usage.setReceiver(IdpSecurityObservationReceiverEnum.APPLICATION);
            observations = new IdpObservedVerificationComponent(producer,
                    Mappers.getMapper(TokenUsageAuditEventConverter.class),
                    usage, metrics::add, clock, validator, SecurityFixture.RESOURCE);
            var settings = identityProperties;
            settings.setResourceServerId("agent-resource"); settings.setResourceUri(SecurityFixture.RESOURCE);
            settings.getRevocation().setPrimaryEndpoint("rediss://redis.example.test:6379"); settings.getRevocation().setEpoch("fixture-epoch");
            var issuers = new LinkedHashMap<String,IdpStarterProperties.TrustedIssuerSettings>();
            for (String tenant : List.of("1", "1001", "1002")) {
                var issuer = new IdpStarterProperties.TrustedIssuerSettings();
                issuer.setIssuer("https://auth.example.test/tenants/"+tenant); issuer.setJwksUri(issuer.getIssuer()+"/.well-known/jwks.json");
                issuer.setRealmKey("tenant:"+tenant); issuer.setRealmType("TENANT"); issuer.setTenantId(tenant); issuer.setStatus("ACTIVE"); issuer.setTrustVersion(1);
                issuers.put("tenant-"+tenant, issuer);
            }
            settings.setTrustedIssuers(issuers);
            var managers = new TenantIssuerAuthenticationManagerComponent(
                    new TrustedIssuerRegistry(settings), settings, clock);
            managers.initialize();
            var rest = (RestTemplate) ReflectionTestUtils.getField(managers, "jwksRestOperations");
            jwks = MockRestServiceServer.bindTo(rest).ignoreExpectOrder(true).build();
            key = new RSAKeyGenerator(2048).keyID("same-kid").algorithm(JWSAlgorithm.RS256).generate();
            for (String tenant : List.of("1", "1001", "1002")) {
                jwks.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo("https://auth.example.test/tenants/"+tenant+"/.well-known/jwks.json"))
                        .andRespond(MockRestResponseCreators.withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
            }
            when(revocations.check(ArgumentMatchers.any())).thenReturn(OAuthRevocationReader.Result.valid());
            var resourceStates = mock(IdentityResourceServerStateReader.class);
            when(resourceStates.read("agent-resource")).thenReturn(Optional.of(new IdentityResourceServerState(
                    "agent-resource", SecurityFixture.RESOURCE, "sample", SecurityFixture.SYSTEM, "test", ResourceServerStatus.ACTIVE, 7)));
            verifier = IdpJwtVerifier.createConfigured(null, resourceStates,
                    mock(IdentityOAuthClientStateReader.class), "agent-resource", SecurityFixture.RESOURCE, "platform", clock,
                    managers, revocations, Mappers.getMapper(VerifiedPrincipalConverter.class), null, observations);
            identityFilter = IdpBearerAuthenticationFilter.create(
                    new UserAccessTokenVerifier(verifier),
                    new ServiceAccessTokenVerifier(verifier), endpointPolicy, mapper,
                    new VerifiedTenantContextFilter(), verifier);
            observationFilter = new IdpSecurityObservationFilter(observations, endpointPolicy);
            properties.setEnabled(true); properties.setRegisterFilter(false); properties.setSystemCode(SecurityFixture.SYSTEM); properties.getUserScopePolicy().setEnabled(true);
            var policy = new LinkedHashMap<String,Set<String>>();
            for (String intent : List.of("research:start", "knowledge-base:create", "knowledge-base:list", "knowledge-base:read", "knowledge-base:update", "knowledge-base:delete", "knowledge-document:upload", "knowledge-document:list", "knowledge-document:read", "knowledge-document:reingest", "knowledge-document:delete", "knowledge:retrieve", "knowledge:chat")) {
                String permission = SYSTEM + ":" + intent;
                policy.put(permission, Set.of(permission)); permissions.add(permission);
                resources.add(SYSTEM + ".api." + intent.replace(':', '.'));
            }
            properties.getUserScopePolicy().setPermissionScopes(policy);
            var store = new AuthorizationSnapshotCache.SnapshotStore() {
                public Optional<SystemAuthorizationSnapshot> get(AuthorizationSnapshotCache.Key key) { return Optional.ofNullable(stored.get(key)); }
                public void put(AuthorizationSnapshotCache.Key key, SystemAuthorizationSnapshot value, Duration ttl) { stored.put(key,value); }
                public void invalidate(AuthorizationSnapshotCache.Key key) { stored.remove(key); }
                public void invalidateUser(String system, String tenant, String subject) { stored.keySet().removeIf(key->key.systemCode().equals(system)&&key.tenantId().equals(tenant)&&key.identitySub().equals(subject)); }
                public void invalidateTenant(String system, String tenant) { stored.keySet().removeIf(key->key.systemCode().equals(system)&&key.tenantId().equals(tenant)); }
            };
            var cache = new AuthorizationSnapshotCache(store, clock, Duration.ZERO);
            var snapshots = new SingleFlightSnapshotLoader(cache, (system, principal)-> {
                if (!snapshotAvailable) throw new Rbac3AuthorizationClient.AuthorizationUnavailableException("PDP_UNAVAILABLE");
                return snapshot(principal);
            }, SecurityFixture.SYSTEM, Duration.ofSeconds(30), clock);
            rbacFilter = new Rbac3BearerAuthenticationFilter(snapshots, mapper);
            var pdp = new DefaultAuthorizationService(()-> {
                permissionChecks++;
                return ((Rbac3ContextAuthentication) SecurityContextHolder.getContext().getAuthentication()).context();
            }, request -> AuthorizationService.OperationSodResult.allowed(),
                    request -> AuthorizationService.FenceResult.allowed(NOW),clock);
            authorization = new Rbac3ObservedAuthorizationService(pdp,metrics::add,observations,validator);
            fields = new Rbac3FieldWriteAuthorizationComponent(authorization,
                    new CurrentRbac3User(),
                    Mappers.getMapper(FieldWriteAuthorizationConverter.class),validator);
            var factory = new StaticListableBeanFactory();
            factory.addBean("observations",observations); factory.addBean("metrics", (IdpSecurityMetricsRecorder) metrics::add); factory.addBean("validator",validator);
            advisor = new Rbac3StarterAutoConfiguration().rbac3MethodAuthorizationInterceptor(authorization, properties,
                    factory.getBeanProvider(IdpObservedVerificationComponent.class),
                    factory.getBeanProvider(IdpSecurityMetricsRecorder.class),factory.getBeanProvider(Validator.class));
        }

        public void clearSnapshots() { stored.clear(); }

        public SystemAuthorizationSnapshot snapshot(IdentityPrincipal principal) {
            var fieldPolicies = new LinkedHashMap<String,FieldPolicyDecision>();
            var writeSets = Map.of(
                    "knowledge-base:create", Set.of("code", "name", "description", "embeddingModel", "chunkStrategy", "chunkConfig"),
                    "knowledge-base:update", Set.of("name", "description"),
                    "knowledge-document:upload", Set.of("file", "displayName"));
            writeSets.forEach((intent, names) -> {
                String permission = SYSTEM + ":" + intent;
                for (String suffix : List.of("api." + intent.replace(':', '.'))) {
                    String resource = SYSTEM + "." + suffix;
                    var facts = names.stream().map(name -> new AuthorizationRuleFacts.FieldRuleFact("501", resource, name, fieldLevel)).toList();
                    if (!explicitFieldRules) { facts = List.of(); }
                    var caps = names.stream().map(name -> new AuthorizationRuleFacts.FieldDefinitionFact(resource, name, FieldAccessLevel.WRITE)).toList();
                    var merged = new FieldPolicyMerger().merge(facts, caps, Set.of("501"));
                    var fields = new LinkedHashMap<String, FieldPolicyDecision.FieldAccess>();
                    names.forEach(name -> fields.put(name, new FieldPolicyDecision.FieldAccess(
                            merged.getOrDefault(resource + "#" + name, FieldAccessLevel.NONE), null)));
                    var decision = new FieldPolicyDecision(Decision.ALLOW, "ALLOW", principal.tenantId(), "2001", permission,
                            SYSTEM, resource, fields, 1, 2, List.of("field-rule-501"), NOW);
                    fieldPolicies.put(permission + ":" + SYSTEM + ":" + resource, decision);
                }
            });
            return new SystemAuthorizationSnapshot(principal.tenantId(),principal.subject(),"2001",SecurityFixture.SYSTEM,
                    1,2,List.of("501"),null,null,Set.copyOf(permissions),Set.copyOf(resources),Map.of(),fieldPolicies,"sha256:fixture",NOW,NOW.plusSeconds(300));
        }

        public Rbac3AuthenticationToken authenticatedUser(String tenant, Set<String> scopes) throws Exception {
            var result = verifier.verifyUser(token(tenant,scopes));
            assertThat(result).isInstanceOf(AccessTokenVerification.Valid.class);
            var verified = (AccessTokenVerification.Valid<?>) result;
            var identity = (IdentityPrincipal) verified.principal();
            var authentication = new Rbac3AuthenticationToken(
                    new Rbac3UserDetails(identity,snapshot(identity)),
                    verified.scopes().stream().map(scope -> new SimpleGrantedAuthority("SCOPE_"+scope)).toList());
            authentication.setDetails(verified.proof()); return authentication;
        }

        public String token(String tenant, Set<String> scopes) throws Exception { return signedToken(tenant,scopes,false); }
        public String serviceToken(String tenant, Set<String> scopes) throws Exception { return signedToken(tenant,scopes,true); }
        private String signedToken(String tenant, Set<String> scopes, boolean service) throws Exception {
            var claims = new LinkedHashMap<String,Object>();
            claims.put("iss","https://auth.example.test/tenants/"+tenant); claims.put("sub",service ? "agent-service" : "subject-"+tenant); claims.put("aud",List.of(SecurityFixture.RESOURCE.toASCIIString()));
            claims.put("iat",NOW.minusSeconds(1).getEpochSecond()); claims.put("nbf",NOW.minusSeconds(1).getEpochSecond()); claims.put("exp",NOW.plusSeconds(300).getEpochSecond());
            claims.put("jti",Long.toString(tokenIds.incrementAndGet())); claims.put("tenant_id",tenant); claims.put("client_id",service ? "agent-service" : "browser-client"); claims.put("principal_type",service ? "SERVICE" : "USER");
            claims.put("scope_context","TENANT"); claims.put("authorization_id","801"); claims.put("client_registration_id","501");
            claims.put("security_version",service ? Map.of("realm",1L,"client",2L) : Map.of("realm",1L,"client",2L,"subject",3L)); claims.put("scope",List.copyOf(scopes));
            if (service) {
                claims.put("resource_version",7L); claims.put("source_biz","sample"); claims.put("source_app",SecurityFixture.SYSTEM); claims.put("source_env","test");
                claims.put("credential_id","credential-1"); claims.put("app_id",SecurityFixture.SYSTEM);
            } else { claims.put("acr","PASSWORD"); claims.put("auth_time",NOW.minusSeconds(1).getEpochSecond()); }
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("same-kid").type(new JOSEObjectType("at+jwt")).build(),JWTClaimsSet.parse(claims));
            jwt.sign(new RSASSASigner(key)); return jwt.serialize();
        }
        @Override public void close() { validator.close(); SecurityContextHolder.clearContext(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); MDC.clear(); }
    }

}
