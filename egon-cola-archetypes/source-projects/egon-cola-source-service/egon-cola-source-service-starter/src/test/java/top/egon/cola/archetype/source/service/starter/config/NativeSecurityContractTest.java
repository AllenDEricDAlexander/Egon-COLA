package top.egon.cola.archetype.source.service.starter.config;

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
import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.Status;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamAddParams;
import org.redisson.client.codec.StringCodec;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import org.slf4j.MDC;
import top.egon.cola.archetype.source.service.facade.proto.CourseServiceGrpc;
import top.egon.cola.archetype.source.service.facade.proto.ExamServiceGrpc;
import top.egon.cola.archetype.source.service.facade.proto.ScoreServiceGrpc;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcBearerServerInterceptor;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

import jakarta.servlet.Filter;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.archetype.source.service.adapter.config.NativeRpcConfiguration;
import top.egon.cola.archetype.source.service.adapter.course.facade.impl.CourseFacadeImpl;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.pojo.convertor.EvaluationFacadeConverter;
import top.egon.cola.archetype.source.service.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.service.application.course.manage.impl.CourseManageImpl;
import top.egon.cola.archetype.source.service.application.course.pojo.convertor.CourseApplicationConverter;
import top.egon.cola.archetype.source.service.application.course.validators.CourseApplicationValidator;
import top.egon.cola.archetype.source.service.domain.course.service.CourseEventService;
import top.egon.cola.archetype.source.service.domain.course.validators.CourseDomainValidator;
import top.egon.cola.archetype.source.service.facade.course.CourseFacade;
import top.egon.cola.archetype.source.service.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.service.infrastructure.course.converter.CourseConverter;
import top.egon.cola.archetype.source.service.infrastructure.course.converter.CourseScheduleConverter;
import top.egon.cola.archetype.source.service.infrastructure.course.po.CoursePO;
import top.egon.cola.archetype.source.service.infrastructure.course.repo.CourseRepository;
import top.egon.cola.archetype.source.service.infrastructure.course.repo.CourseScheduleRepository;
import top.egon.cola.archetype.source.service.infrastructure.course.service.impl.CourseDomainServiceImpl;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceScopeAuthorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real provider, application/domain conversion and host policy; only the data port is controlled. */
class NativeSecurityContractTest {
    @Test
    void frozenFacadeSchemaAndOperationIdentitiesRemainTheOriginalContract() throws Exception {
        var proto = Path.of("../egon-cola-source-service-facade/src/main/proto/evaluation_facade.proto");
        String testPackage = getClass().getPackageName();
        String javaPackage = testPackage.substring(0, testPackage.length() - ".starter.config".length())
                + ".facade.proto";
        String declaration = "option java_package = \"" + javaPackage + "\";";
        String source = Files.readString(proto);
        assertThat(source).containsOnlyOnce(declaration);
        String normalized = source.replace(declaration, "option java_package = \"<project-package>\";");
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(hash).isEqualTo("b0be505ae4df8b422cacb9334aaffe9be55bff478127fc35d2130126f86264a1");
        assertThat(CourseServiceGrpc.getServiceDescriptor().getName()).isEqualTo("top.egon.cola.evaluation.facade.rpc.v1.CourseService");
        assertThat(ExamServiceGrpc.getServiceDescriptor().getName()).isEqualTo("top.egon.cola.evaluation.facade.rpc.v1.ExamService");
        assertThat(ScoreServiceGrpc.getServiceDescriptor().getName()).isEqualTo("top.egon.cola.evaluation.facade.rpc.v1.ScoreService");
    }

    @org.junit.jupiter.api.Nested
    @org.springframework.test.context.ActiveProfiles("test")
    @org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
    @org.springframework.boot.test.context.SpringBootTest(
            classes = top.egon.cola.archetype.source.service.starter.EvaluationServiceApplication.class,
            properties = {"egon.cola.component.yuheng.openapi.enabled=true",
                    "egon.cola.component.yuheng.openapi.publish-to-tianshu=false",
                    "egon.cola.component.yuheng.openapi.published-groups[0]=public",
                    "egon.cola.component.yuheng.openapi.biz-code=student-management",
                    "egon.cola.component.yuheng.openapi.application-code=egon-cola-source-service",
                    "egon.cola.component.yuheng.openapi.resource-uri=https://evaluation.example.test",
                    "egon.cola.component.yuheng.openapi.artifact-version=1.0.0",
                    "egon.cola.component.yuheng.openapi.build-id=contract-test",
                    "springdoc.group-configs[0].group=public",
                    "springdoc.group-configs[0].paths-to-match=/actuator/**"})
    @org.springframework.context.annotation.Import(DocumentFixtureConfiguration.class)
    class ServiceDocumentChain extends top.egon.cola.archetype.source.service.support.PersistenceTestSupport {
        @org.springframework.beans.factory.annotation.Autowired
        private org.springframework.test.web.servlet.MockMvc mvc;
        @org.springframework.beans.factory.annotation.Autowired
        private SecurityFixture security;

        @Test
        void actualGeneratedDocumentsUseTheNarrowServiceChainAndHealthKeepsItsOwnPolicy() throws Exception {
            mvc.perform(get("/v3/api-docs/public")).andExpect(status().isUnauthorized());
            mvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + security.token("1001", Set.of("unrelated"), true)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + security.token("1001", Set.of("yuheng.openapi.read"), false)))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + security.token("1001", Set.of("yuheng.openapi.read"), true)))
                    .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.openapi").exists());
            mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
            mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
            mvc.perform(get("/internal-contract-probe")).andExpect(status().isForbidden());
            when(security.revocations.check(ArgumentMatchers.any())).thenReturn(OAuthRevocationReader.Result.unavailable("OAUTH_REVOCATION_UNAVAILABLE"));
            mvc.perform(get("/v3/api-docs/public").header("Authorization", "Bearer " + security.token("1001", Set.of("yuheng.openapi.read"), true)))
                    .andExpect(status().isServiceUnavailable());
        }
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class DocumentFixtureConfiguration {
        @org.springframework.context.annotation.Bean(name = "nativeSecurityFixture", destroyMethod = "close")
        SecurityFixture fixture() throws Exception { return new SecurityFixture(Files.createTempDirectory("service-security-test-")); }
        @org.springframework.context.annotation.Bean("idpJwtVerifier")
        IdpJwtVerifier verifier(SecurityFixture fixture) { return fixture.verifier; }
        @org.springframework.context.annotation.Bean("userAccessTokenVerifier")
        UserAccessTokenVerifier users(SecurityFixture fixture) { return new UserAccessTokenVerifier(fixture.verifier); }
        @org.springframework.context.annotation.Bean("serviceAccessTokenVerifier")
        top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceAccessTokenVerifier services(SecurityFixture fixture) {
            return new top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceAccessTokenVerifier(fixture.verifier);
        }
        @org.springframework.context.annotation.Bean("idpObservedVerificationComponent")
        IdpObservedVerificationComponent observations(SecurityFixture fixture) { return fixture.observations; }
    }

    @Test
    void signedServiceUsesTheActualProxyReadChainAndRestoresTrustedActor(@TempDir Path temporary) throws Exception {
        var repository = mock(CourseRepository.class);
        var row = new CoursePO().setCode("MATH").setName("Math").setCredit(2).setStatus("ACTIVE");
        row.setId(1002L);
        when(repository.findCachedById(1002L)).thenAnswer(invocation -> {
            assertThat(MDC.get("tenantId")).isEqualTo("1002");
            assertThat(MDC.get("userId")).isEqualTo("evaluation-service");
            assertThat(MDC.get("egon.auth.actor-kind")).isEqualTo("SERVICE");
            return row;
        });
        try (var fixture = new SecurityFixture(temporary)) {
            providerRunner(repository, new ValidationUtils(fixture.validator))
                    .withBean("idpObservedVerificationComponent", IdpObservedVerificationComponent.class, () -> fixture.observations)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        String token = fixture.token("1002", Set.of(SecurityFixture.SYSTEM + ":course:read"), true);
                        fixture.properties.getRpc().setEnabled(true);
                        var policies = providerPolicies(); policies.setEnabled(true);
                        fixture.properties.getRpc().setProvider(policies);
                        var interceptor = IdpRpcBearerServerInterceptor.createConfigured(new UserAccessTokenVerifier(fixture.verifier),
                                fixture.verifier, fixture.properties, fixture.observations, fixture.validator);
                        @SuppressWarnings("unchecked")
                        ServerCall<GetCourseRpcRequest, top.egon.cola.archetype.source.service.facade.proto.CourseRpcResponse> call = mock(ServerCall.class);
                        when(call.getMethodDescriptor()).thenReturn(CourseServiceGrpc.getGetCourseMethod());
                        when(call.getAttributes()).thenReturn(Attributes.EMPTY);
                        var metadata = new Metadata();
                        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);
                        metadata.put(Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER), "9999");
                        var received = new AtomicReference<top.egon.cola.archetype.source.service.facade.proto.CourseRpcResponse>();
                        MDC.put("tenantId", "previous");
                        MDC.put("userId", "previous-actor");
                        var listener = interceptor.interceptCall(call, metadata, (server, headers) -> new ServerCall.Listener<>() {
                            @Override public void onHalfClose() {
                                received.set(context.getBean(CourseFacade.class)
                                        .getCourse(GetCourseRpcRequest.newBuilder().setCourseId(1002L).build()));
                                assertThat(IdpRpcSecurityContext.currentTokenOrNull()).isNull();
                            }
                        });
                        listener.onHalfClose(); listener.onComplete();
                        assertThat(received.get().getSuccess()).isTrue();
                        assertThat(received.get().getData().getCode()).isEqualTo("MATH");
                        assertThat(MDC.get("tenantId")).isEqualTo("previous");
                        assertThat(MDC.get("userId")).isEqualTo("previous-actor");
                        assertThat(IdpRpcSecurityContext.currentPrincipalOrNull()).isNull();
                        assertThat(fixture.mapper.writeValueAsString(fixture.events)).doesNotContain(token);
                    });
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void everyFullMethodUsesExactServiceTenantPolicyAndRejectsBeforeCallback(@TempDir Path temporary) throws Exception {
        try (var fixture = new SecurityFixture(temporary)) {
            var descriptors = List.of(CourseServiceGrpc.getCreateCourseMethod(), CourseServiceGrpc.getScheduleCourseMethod(),
                    CourseServiceGrpc.getGetCourseMethod(), CourseServiceGrpc.getPageCoursesMethod(),
                    ExamServiceGrpc.getCreateExamMethod(), ExamServiceGrpc.getAttachPaperMethod(),
                    ExamServiceGrpc.getPublishExamMethod(), ExamServiceGrpc.getGetExamMethod(),
                    ScoreServiceGrpc.getRecordScoreMethod(), ScoreServiceGrpc.getGetScoreMethod(), ScoreServiceGrpc.getPageScoresMethod());
            var policies = providerPolicies();
            fixture.properties.getRpc().setEnabled(true);
            policies.setEnabled(true);
            fixture.properties.getRpc().setProvider(policies);
            var interceptor = IdpRpcBearerServerInterceptor.createConfigured(new UserAccessTokenVerifier(fixture.verifier),
                    fixture.verifier, fixture.properties, fixture.observations, fixture.validator);
            for (MethodDescriptor descriptor : descriptors) {
                var policy = policies.getMethods().get(descriptor.getFullMethodName());
                assertThat(policy).isNotNull();
                String accepted = fixture.token("1001", policy.getRequiredScopes(), true);
                var cases = new LinkedHashMap<String, Status.Code>();
                cases.put("", Status.Code.UNAUTHENTICATED);
                cases.put("malformed", Status.Code.UNAUTHENTICATED);
                cases.put(fixture.token("1001", policy.getRequiredScopes(), false), Status.Code.PERMISSION_DENIED);
                cases.put(fixture.token("1001", Set.of("unrelated"), true), Status.Code.PERMISSION_DENIED);
                cases.put(fixture.token("1001", policy.getRequiredScopes(), true,
                        claims -> claims.put("aud", List.of("https://foreign.example.test"))), Status.Code.UNAUTHENTICATED);
                cases.put(fixture.token("1001", policy.getRequiredScopes(), true,
                        claims -> claims.put("tenant_id", "1002")), Status.Code.UNAUTHENTICATED);
                cases.put(fixture.token("1001", policy.getRequiredScopes(), true,
                        claims -> claims.put("exp", SecurityFixture.NOW.minusSeconds(60).getEpochSecond())), Status.Code.UNAUTHENTICATED);
                cases.put(accepted, Status.Code.OK);
                for (var request : cases.entrySet()) {
                    ServerCall call = mock(ServerCall.class);
                    when(call.getMethodDescriptor()).thenReturn(descriptor);
                    when(call.getAttributes()).thenReturn(Attributes.EMPTY);
                    var closed = new AtomicReference<Status>();
                    Mockito.doAnswer(invocation -> { closed.set(invocation.getArgument(0)); return null; })
                            .when(call).close(ArgumentMatchers.any(), ArgumentMatchers.any());
                    var metadata = new Metadata();
                    if (!request.getKey().isEmpty()) {
                        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + request.getKey());
                    }
                    metadata.put(Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER), "9999");
                    var entered = new AtomicInteger();
                    var marker = new AtomicReference<IdpAuthorizationObservationContextBO>();
                    var previous = SecurityContextHolder.getContext();
                    var listener = interceptor.interceptCall(call, metadata, (server, headers) -> {
                        entered.incrementAndGet();
                        assertThat(MDC.get("tenantId")).isEqualTo("1001");
                        assertThat(MDC.get("userId")).isEqualTo("evaluation-service");
                        assertThat(IdpRpcSecurityContext.currentTokenOrNull()).isNull();
                        assertThat(IdpRpcSecurityContext.currentProofOrNull()).isNotNull();
                        marker.set(IdpRpcSecurityContext.currentAuthorizationObservationContext());
                        return new ServerCall.Listener() {
                            @Override public void onHalfClose() { assertThat(MDC.get("tenantId")).isEqualTo("1001"); }
                        };
                    });
                    if (request.getValue() == Status.Code.OK) {
                        assertThat(entered.get()).isEqualTo(1);
                        listener.onHalfClose(); listener.onCancel();
                        assertThat(marker.get().isActive()).isFalse();
                    } else {
                        assertThat(entered.get()).isZero();
                        assertThat(closed.get().getCode()).isEqualTo(request.getValue());
                    }
                    assertThat(SecurityContextHolder.getContext()).isSameAs(previous);
                    assertThat(MDC.get("tenantId")).isNull();
                }
            }
        }
    }

    static IdpStarterProperties.RpcProviderProperties providerPolicies() throws Exception {
        var values = new LinkedHashMap<String, Object>();
        for (var source : new org.springframework.boot.env.YamlPropertySourceLoader().load("methods",
                new org.springframework.core.io.ClassPathResource("application-test.yml"))) {
            ((org.springframework.core.env.MapPropertySource) source).getSource().forEach((key, value) -> {
                if (key.startsWith("egon.cola.platform.tianquan.shoubing.rpc.provider.")) {
                    Object raw = value instanceof org.springframework.boot.origin.OriginTrackedValue tracked ? tracked.getValue() : value;
                    values.put(key, key.endsWith("expected-resource-uri") ? SecurityFixture.RESOURCE.toASCIIString() : raw);
                }
            });
        }
        var binder = new org.springframework.boot.context.properties.bind.Binder(
                new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(values));
        return binder.bind("egon.cola.platform.tianquan.shoubing.rpc.provider",
                org.springframework.boot.context.properties.bind.Bindable.of(IdpStarterProperties.RpcProviderProperties.class)).get();
    }

    @Test
    void fourProfilesBindElevenProvidersAndOnlyTwoOrganizationReads() throws Exception {
        Set<String> baseline = null;
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            for (String profile : List.of("application.yml", "application-dev.yml", "application-prod.yml", "application-test.yml")) {
                var values = new LinkedHashMap<String, Object>();
                for (var source : new org.springframework.boot.env.YamlPropertySourceLoader().load(profile,
                        new org.springframework.core.io.ClassPathResource(profile))) {
                    ((org.springframework.core.env.MapPropertySource) source).getSource().forEach((key, value) -> {
                        Object raw = value instanceof org.springframework.boot.origin.OriginTrackedValue tracked ? tracked.getValue() : value;
                        values.put(key, raw);
                    });
                }
                var keys = values.keySet().stream().filter(key -> key.startsWith("egon.cola.platform.tianquan.shoubing.")
                                || key.startsWith("spring.security.oauth2.client.") || key.startsWith("server.ssl."))
                        .filter(key -> !key.contains("tenant-registration-ids"))
                        .collect(java.util.stream.Collectors.toSet());
                if (baseline == null) { baseline = keys; } else { assertThat(keys).as(profile).isEqualTo(baseline); }
                values.put("TIANQUAN_SHOUBING_RESOURCE_URI", SecurityFixture.RESOURCE.toASCIIString());
                values.put("ORGANIZATION_RESOURCE_URI", "https://organization.example.test");
                values.put("spring.application.name", "evaluation-app");
                values.put("APP_ENV", "test");
                values.put("app.integrations.organization.application-namespace", "organization-app");
                values.put("app.integrations.organization.group", "student-management-organization");
                values.put("app.integrations.organization.version", "1.0.0");
                for (String key : List.of("organization-user-read", "organization-class-read")) {
                    values.put("egon.cola.platform.tianquan.shoubing.rpc.consumer.bindings." + key + ".tenant-registration-ids[1001]", "native-tenant-one");
                }
                var environment = new org.springframework.core.env.StandardEnvironment();
                environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("contract", values));
                var rpc = org.springframework.boot.context.properties.bind.Binder.get(environment)
                        .bind("egon.cola.platform.tianquan.shoubing.rpc", IdpStarterProperties.RpcProperties.class).get();
                assertThat(validators.getValidator().validate(rpc)).as(profile).isEmpty();
                assertThat(rpc.getProvider().getMethods()).hasSize(11);
                rpc.getProvider().getMethods().values().forEach(policy -> {
                    assertThat(policy.isRequired()).isTrue();
                    assertThat(policy.getPrincipalKind()).isEqualTo(IdpStarterProperties.RpcPrincipalKindEnum.SERVICE);
                    assertThat(policy.getScopeContext()).isEqualTo(top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT);
                    assertThat(policy.getRequiredScopes()).hasSize(1).allSatisfy(scope -> assertThat(scope).startsWith(SecurityFixture.SYSTEM + ":"));
                });
                assertThat(rpc.getConsumer().getBindings()).hasSize(2);
                rpc.getConsumer().getBindings().values().forEach(binding -> {
                    assertThat(binding.getMode()).isEqualTo(IdpStarterProperties.RpcConsumerModeEnum.SERVICE);
                    assertThat(binding.getRegistrationId()).isNull();
                    assertThat(binding.getResourceUri()).isEqualTo(URI.create("https://organization.example.test"));
                    assertThat(binding.getProcessIdentity()).containsEntry("applicationName", "evaluation-app").containsEntry("env", "test");
                    assertThat(binding.getScopes()).hasSize(1).allSatisfy(scope -> assertThat(scope).startsWith("organization-app:"));
                });
            }
        }
    }


    @Test
    void missingServiceIdentityStopsTheActualProviderBeforeItsDataRead() {
        var repository = mock(CourseRepository.class);
        var row = new CoursePO().setCode("MATH").setName("Math").setCredit(2).setStatus("ACTIVE");
        row.setId(1002L);
        when(repository.findCachedById(1002L)).thenReturn(row);
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var validation = new ValidationUtils(validators.getValidator());
            providerRunner(repository, validation).run(context -> {
                assertThat(context).hasNotFailed();
                SecurityContextHolder.clearContext();
                assertThatThrownBy(() -> context.getBean(CourseFacade.class)
                        .getCourse(GetCourseRpcRequest.newBuilder().setCourseId(1002L).build()))
                        .hasRootCauseInstanceOf(AuthenticationException.class);
                verifyNoInteractions(repository);
            });
        }
    }

    @Test
    void infrastructureFallbackCannotMakeDisabledDocumentsPublic() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
                        SecurityFilterAutoConfiguration.class, WebMvcAutoConfiguration.class))
                .withUserConfiguration(NativeHttpSecurityConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    MockMvcBuilders.webAppContextSetup(context)
                            .addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build()
                            .perform(get("/v3/api-docs")).andExpect(status().isForbidden());
                });
    }

    static ApplicationContextRunner providerRunner(CourseRepository repository, ValidationUtils validation) {
        var domain = new CourseDomainServiceImpl(repository, mock(CourseScheduleRepository.class),
                Mappers.getMapper(CourseConverter.class), Mappers.getMapper(CourseScheduleConverter.class),
                new CourseDomainValidator(validation));
        CourseManage manage = new CourseManageImpl(mock(CourseEventService.class), domain,
                Mappers.getMapper(CourseApplicationConverter.class), new CourseApplicationValidator(validation));
        return new ApplicationContextRunner()
                .withUserConfiguration(NativeRpcConfiguration.class, MethodSecurityConfiguration.class)
                .withBean("egonColaValidationUtils", ValidationUtils.class, () -> validation)
                .withBean("serviceScopeAuthorization", ServiceScopeAuthorization.class, ServiceScopeAuthorization::new)
                .withBean("courseFacadeImpl", CourseFacadeImpl.class, () -> new CourseFacadeImpl(manage,
                        Mappers.getMapper(EvaluationFacadeConverter.class), validation, new GlobalFacadeExceptionHandler()));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
    /** Actual RS256 multi-issuer verification and strict producer; only JWK transport/state stores are controlled. */
    static final class SecurityFixture implements AutoCloseable {
        static final String SYSTEM = "egon-cola-source-service";
        static final URI RESOURCE = URI.create("https://evaluation.example.test");
        static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
        final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        final IdpStarterProperties properties = new IdpStarterProperties();
        final OAuthRevocationReader revocations = mock(OAuthRevocationReader.class);
        final IdentityResourceServerStateReader resourceStates = mock(IdentityResourceServerStateReader.class);
        final List<Map<String, String>> events = new CopyOnWriteArrayList<>();
        final List<SecurityMetricObservationBO> metrics = new CopyOnWriteArrayList<>();
        final IdpJwtVerifier verifier;
        final IdpObservedVerificationComponent observations;
        final RSAKey key;
        private final AtomicLong tokenIds = new AtomicLong(900);

        @SuppressWarnings("unchecked")
        SecurityFixture(Path root) throws Exception {
            validator.afterPropertiesSet();
            var keyFile = root.resolve("audit.key");
            Files.writeString(keyFile, "0123456789abcdef0123456789abcdef");
            Files.setPosixFilePermissions(keyFile, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            var redis = mock(RedissonClient.class);
            RStream<String, String> stream = mock(RStream.class);
            Mockito.doReturn(stream).when(redis).getStream(ArgumentMatchers.eq("egon:oauth:audit:v1"), ArgumentMatchers.eq(StringCodec.INSTANCE));
            when(stream.add(ArgumentMatchers.any(StreamAddArgs.class))).thenAnswer(invocation -> {
                StreamAddParams<String, String> append = invocation.getArgument(0);
                events.add(Map.copyOf(append.getEntries()));
                return new StreamMessageId(events.size(), 1);
            });
            var producer = new TokenUsageAuditComponent(redis, mapper,
                    new OAuthAuditProperties("egon:oauth:audit:v1", root.resolve("spool"), 1048576, keyFile),
                    new AuditDeliveryProperties(), clock, validator);
            var usage = new OAuthUsageObservationProperties(true, URI.create("https://auth.example.test/tenants/1001"), false);
            usage.setReceiver(IdpSecurityObservationReceiverEnum.APPLICATION);
            observations = new IdpObservedVerificationComponent(producer,
                    Mappers.getMapper(TokenUsageAuditEventConverter.class), usage, metrics::add, clock, validator, RESOURCE);
            properties.setResourceServerId("evaluation-resource");
            properties.setResourceUri(RESOURCE);
            properties.getRevocation().setPrimaryEndpoint("rediss://redis.example.test:6379");
            properties.getRevocation().setEpoch("fixture-epoch");
            var issuers = new LinkedHashMap<String, IdpStarterProperties.TrustedIssuerSettings>();
            for (String tenant : List.of("1001", "1002")) {
                var issuer = new IdpStarterProperties.TrustedIssuerSettings();
                issuer.setIssuer("https://auth.example.test/tenants/" + tenant);
                issuer.setJwksUri(issuer.getIssuer() + "/.well-known/jwks.json");
                issuer.setRealmKey("tenant:" + tenant);
                issuer.setRealmType("TENANT");
                issuer.setTenantId(tenant);
                issuer.setStatus("ACTIVE");
                issuer.setTrustVersion(1);
                issuers.put(tenant, issuer);
            }
            properties.setTrustedIssuers(issuers);
            var managers = new TenantIssuerAuthenticationManagerComponent(new TrustedIssuerRegistry(properties), properties, clock);
            managers.initialize();
            var rest = (RestTemplate) ReflectionTestUtils.getField(managers, "jwksRestOperations");
            var jwks = MockRestServiceServer.bindTo(rest).ignoreExpectOrder(true).build();
            key = new RSAKeyGenerator(2048).keyID("same-kid").algorithm(JWSAlgorithm.RS256).generate();
            for (String tenant : List.of("1001", "1002")) {
                jwks.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo("https://auth.example.test/tenants/" + tenant + "/.well-known/jwks.json"))
                        .andRespond(MockRestResponseCreators.withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
            }
            when(revocations.check(ArgumentMatchers.any())).thenReturn(OAuthRevocationReader.Result.valid());
            when(resourceStates.read("evaluation-resource")).thenReturn(Optional.of(new IdentityResourceServerState(
                    "evaluation-resource", RESOURCE, "student-management", SYSTEM, "test", ResourceServerStatus.ACTIVE, 7)));
            verifier = IdpJwtVerifier.createConfigured(null, resourceStates, mock(IdentityOAuthClientStateReader.class),
                    "evaluation-resource", RESOURCE, "platform", clock, managers, revocations,
                    Mappers.getMapper(VerifiedPrincipalConverter.class), null, observations);
        }

        String token(String tenant, Set<String> scopes, boolean service) throws Exception {
            return token(tenant, scopes, service, claims -> {});
        }

        String token(String tenant, Set<String> scopes, boolean service, java.util.function.Consumer<Map<String, Object>> amend) throws Exception {
            var claims = new LinkedHashMap<String, Object>();
            claims.put("iss", "https://auth.example.test/tenants/" + tenant);
            claims.put("sub", service ? "evaluation-service" : "subject-" + tenant);
            claims.put("aud", List.of(RESOURCE.toASCIIString()));
            claims.put("iat", NOW.minusSeconds(1).getEpochSecond());
            claims.put("nbf", NOW.minusSeconds(1).getEpochSecond());
            claims.put("exp", NOW.plusSeconds(300).getEpochSecond());
            claims.put("jti", Long.toString(tokenIds.incrementAndGet()));
            claims.put("tenant_id", tenant);
            claims.put("client_id", service ? "evaluation-service" : "browser-client");
            claims.put("principal_type", service ? "SERVICE" : "USER");
            claims.put("scope_context", "TENANT");
            claims.put("authorization_id", "801");
            claims.put("client_registration_id", "501");
            claims.put("security_version", service ? Map.of("realm", 1L, "client", 2L)
                    : Map.of("realm", 1L, "client", 2L, "subject", 3L));
            claims.put("scope", List.copyOf(scopes));
            if (service) {
                claims.put("resource_version", 7L);
                claims.put("source_biz", "student-management");
                claims.put("source_app", SYSTEM);
                claims.put("source_env", "test");
                claims.put("credential_id", "credential-one");
                claims.put("app_id", SYSTEM);
            } else {
                claims.put("acr", "PASSWORD");
                claims.put("auth_time", NOW.minusSeconds(1).getEpochSecond());
            }
            amend.accept(claims);
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("same-kid")
                    .type(new JOSEObjectType("at+jwt")).build(), JWTClaimsSet.parse(claims));
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        }

        @Override
        public void close() {
            validator.close();
            SecurityContextHolder.clearContext();
            MDC.clear();
        }
    }

}
