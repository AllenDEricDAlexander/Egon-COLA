package top.egon.cola.archetype.source.light.start.config;

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
import jakarta.servlet.Filter;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamAddParams;
import org.redisson.client.codec.StringCodec;
import org.slf4j.MDC;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContextFilter;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.light.adapter.pojo.convertor.LightFacadeConverter;
import top.egon.cola.archetype.source.light.adapter.teaching.facade.impl.CourseFacadeImpl;
import top.egon.cola.archetype.source.light.adapter.teaching.controller.CourseController;
import top.egon.cola.archetype.source.light.adapter.teaching.controller.SchoolClassController;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.convertor.TeachingAdapterConvertor;
import top.egon.cola.archetype.source.light.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.cola.archetype.source.light.adapter.user.controller.PermissionController;
import top.egon.cola.archetype.source.light.adapter.user.controller.RoleController;
import top.egon.cola.archetype.source.light.adapter.user.controller.UserController;
import top.egon.cola.archetype.source.light.adapter.user.pojo.convertor.UserAdapterConvertorImpl;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.application.user.manage.impl.UserManageImpl;
import top.egon.cola.archetype.source.light.application.user.pojo.convertor.UserApplicationConvertorImpl;
import top.egon.cola.archetype.source.light.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.light.domain.user.service.UserEventService;
import top.egon.cola.archetype.source.light.domain.user.service.UserIdempotencyService;
import top.egon.cola.archetype.source.light.domain.user.service.UserQueryService;
import top.egon.cola.archetype.source.light.facade.proto.CourseServiceGrpc;
import top.egon.cola.archetype.source.light.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.PermissionServiceGrpc;
import top.egon.cola.archetype.source.light.facade.proto.SchoolClassServiceGrpc;
import top.egon.cola.archetype.source.light.facade.proto.UserServiceGrpc;
import top.egon.cola.archetype.source.light.facade.teaching.CourseFacade;
import top.egon.cola.archetype.source.light.infrastructure.user.converter.UserPOConverterImpl;
import top.egon.cola.archetype.source.light.infrastructure.user.po.UserPO;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.UserRepository;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.UserRoleRepository;
import top.egon.cola.archetype.source.light.infrastructure.user.service.impl.UserDomainServiceImpl;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.rpc.common.exception.EgonRpcRejectedException;
import top.egon.cola.component.rpc.provider.server.RpcProviderExceptionMapper;
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
import top.egon.cola.platform.tianquan.jianshen.starter.field.FieldWriteAuthorizationCommand;
import top.egon.cola.platform.tianquan.jianshen.starter.field.Rbac3FieldWriteAuthorizationComponent;
import top.egon.cola.platform.tianquan.jianshen.starter.security.CurrentRbac3User;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3BearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3ContextAuthentication;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.jianshen.starter.web.Rbac3AuthorizationExceptionHandler;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
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
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceScopeAuthorization;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedTenantContextFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcBearerServerInterceptor;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcSecurityContext;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Actual host chain and business conversion; only data and unused external delivery ports are substituted. */
public class NativeSecurityContractTest {
    @Test
    void allFourProfilesBindExactProviderAndUserScopeMaps() throws Exception {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            Set<String> baselineKeys = null;
            for (String profile : List.of("application.yml", "application-dev.yml", "application-prod.yml", "application-test.yml")) {
                var values = new LinkedHashMap<String,Object>();
                for (var source : new YamlPropertySourceLoader().load(profile, new ClassPathResource(profile))) {
                    ((MapPropertySource) source).getSource().forEach((key, value) -> {
                        if (key.startsWith("egon.cola.platform.tianquan.") || key.startsWith("spring.security.oauth2.client.") || key.startsWith("server.ssl.")) {
                            Object raw = value instanceof OriginTrackedValue tracked ? tracked.getValue() : value;
                            if (key.endsWith("expected-resource-uri")) { raw = SecurityFixture.RESOURCE.toASCIIString(); }
                            values.put(key, raw);
                        }
                    });
                }
                var keys = Set.copyOf(values.keySet());
                if (baselineKeys == null) { baselineKeys = keys; } else { assertThat(keys).as("Canonical key parity for %s", profile).isEqualTo(baselineKeys); }
                var binder = new Binder(new MapConfigurationPropertySource(values));
                var provider = binder.bind("egon.cola.platform.tianquan.shoubing.rpc.provider", Bindable.of(
                        IdpStarterProperties.RpcProviderProperties.class)).get();
                assertThat(validator.validate(provider)).as(profile).isEmpty();
                assertThat(provider.getMethods()).hasSize(10);
                provider.getMethods().forEach((method, policy) -> {
                    assertThat(method).startsWith("top.egon.cola.archetype.source.light.facade.rpc.v1.").contains("/");
                    assertThat(policy.isRequired()).isTrue(); assertThat(policy.getPrincipalKind().name()).isEqualTo("SERVICE");
                    assertThat(policy.getScopeContext().name()).isEqualTo("TENANT"); assertThat(policy.getExpectedResourceUri()).isEqualTo(SecurityFixture.RESOURCE);
                    assertThat(policy.getRequiredScopes()).hasSize(1).allSatisfy(scope -> assertThat(scope).startsWith(SecurityFixture.SYSTEM + ":"));
                });
                var userPolicy = binder.bind("egon.cola.platform.tianquan.jianshen.user-scope-policy", Bindable.of(
                        Rbac3StarterProperties.UserScopePolicyProperties.class)).get();
                assertThat(validator.validate(userPolicy)).as(profile).isEmpty();
                assertThat(userPolicy.getPermissionScopes()).hasSize(10);
                userPolicy.getPermissionScopes().forEach((permission, scopes) -> assertThat(scopes).containsExactly(permission));
            }
        }
    }

    @Test
    void missingBearerIsRejectedBeforeTheRealUserReadChain(@TempDir Path temporary) throws Exception {
        var repository = mock(UserRepository.class);
        var row = UserPO.builder().externalId("external-1").name("Reader").email("reader@example.test").status("ACTIVE").build();
        row.setId(1001L);
        when(repository.findCachedById(1001L)).thenReturn(row);
        try (var fixture = new SecurityFixture(temporary)) {
            runner(fixture, repository).run(context -> {
                assertThat(context).hasNotFailed();
                var mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
                mvc.perform(get("/api/users/1001")).andExpect(status().isUnauthorized());
                verify(repository, never()).findCachedById(1001L);
            });
        }
    }

    @Test
    void signedUserNeedsBothScopeAndActualPdpPermission(@TempDir Path temporary) throws Exception {
        var repository = mock(UserRepository.class);
        var row = UserPO.builder().externalId("external-1").name("Reader").email("reader@example.test").status("ACTIVE").build();
        row.setId(1001L); when(repository.findCachedById(1001L)).thenReturn(row);
        try (var fixture = new SecurityFixture(temporary)) {
            runner(fixture, repository).run(context -> {
                assertThat(context).hasNotFailed();
                var mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
                String wrongScope = fixture.token("1001", Set.of("openid"));
                mvc.perform(get("/api/users/1001").header("Authorization", "Bearer " + wrongScope))
                        .andExpect(status().isForbidden()).andExpect(MockMvcResultMatchers.header().string("Cache-Control", "no-store"));
                verify(repository, never()).findCachedById(1001L);
                assertThat(fixture.permissionChecks).as("Scope precedes the real PDP").isZero();
                String permitted = fixture.token("1002", Set.of(SecurityFixture.SYSTEM + ":user:read"));
                mvc.perform(get("/api/users/1001").header("Authorization", "Bearer " + permitted)
                                .header("X-Tenant-Id", "9999").header("X-Operator-Id", "forged-actor"))
                        .andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Reader"));
                verify(repository).findCachedById(1001L);
                assertThat(fixture.permissionChecks).isGreaterThan(0);
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                assertThat(RequestContextHolder.get()).isEmpty();
                assertThat(MDC.get("tenantId")).isNull();
                assertThat(fixture.events).anySatisfy(event -> assertThat(event.get("reasonCode")).isEqualTo("TOKEN_USE_VERIFIED"));
                assertThat(fixture.mapper.writeValueAsString(fixture.events)).doesNotContain(wrongScope, permitted, "subject-1002");
            });
        }
    }

    @Test
    void correctScopeCannotReplaceMissingPermission(@TempDir Path temporary) throws Exception {
        var repository = mock(UserRepository.class);
        try (var fixture = new SecurityFixture(temporary)) {
            fixture.permissions.remove(SecurityFixture.SYSTEM + ":user:read");
            runner(fixture, repository).run(context -> {
                assertThat(context).hasNotFailed();
                var mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
                mvc.perform(get("/api/users/1001").header("Authorization", "Bearer " + fixture.token("1001", Set.of(SecurityFixture.SYSTEM + ":user:read"))))
                        .andExpect(status().isForbidden());
                verify(repository, never()).findCachedById(1001L);
                assertThat(fixture.permissionChecks).isGreaterThan(0);
            });
        }
    }

    @Test
    void unavailableSnapshotCannotTurnIntoDeniedOrBusinessSuccess(@TempDir Path temporary) throws Exception {
        var repository = mock(UserRepository.class);
        try (var fixture = new SecurityFixture(temporary)) {
            fixture.snapshotAvailable = false;
            runner(fixture, repository).run(context -> {
                assertThat(context).hasNotFailed();
                var mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
                mvc.perform(get("/api/users/1001").header("Authorization", "Bearer " + fixture.token("1001", Set.of(SecurityFixture.SYSTEM + ":user:read"))))
                        .andExpect(status().isServiceUnavailable());
                verify(repository, never()).findCachedById(1001L);
            });
        }
    }

    @Test
    void strictRevocationFailureStopsTheActualVerifierBeforeBusiness(@TempDir Path temporary) throws Exception {
        var repository = mock(UserRepository.class);
        try (var fixture = new SecurityFixture(temporary)) {
            when(fixture.revocations.check(ArgumentMatchers.any())).thenReturn(OAuthRevocationReader.Result.unavailable("OAUTH_REVOCATION_UNAVAILABLE"));
            runner(fixture, repository).run(context -> {
                assertThat(context).hasNotFailed();
                var mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
                mvc.perform(get("/api/users/1001").header("Authorization", "Bearer " + fixture.token("1001", Set.of(SecurityFixture.SYSTEM + ":user:read"))))
                        .andExpect(status().isServiceUnavailable());
                verify(repository, never()).findCachedById(1001L);
            });
        }
    }

    @Test
    void everyRestEntryRejectsMissingCredentialsAndMissingScopeBeforeItsUseCase(@TempDir Path temporary) throws Exception {
        var users = mock(UserRepository.class);
        var courses = mock(CourseManage.class);
        var classes = mock(SchoolClassManage.class);
        var roles = mock(RoleManage.class);
        var permissions = mock(PermissionManage.class);
        try (var fixture = new SecurityFixture(temporary)) {
            runner(fixture, users)
                    .withBean("courseManageImpl", CourseManage.class, () -> courses)
                    .withBean("schoolClassManageImpl", SchoolClassManage.class, () -> classes)
                    .withBean("roleManageImpl", RoleManage.class, () -> roles)
                    .withBean("permissionManageImpl", PermissionManage.class, () -> permissions)
                    .withBean("teachingDomainCourseController", CourseController.class,
                            () -> new CourseController(courses,
                                    Mappers.getMapper(TeachingAdapterConvertor.class), fixture.fields, fixture.properties))
                    .withBean("schoolClassController", SchoolClassController.class,
                            () -> new SchoolClassController(classes,
                                    new TeachingRequestValidator(new ValidationUtils(fixture.validator)),
                                    Mappers.getMapper(TeachingAdapterConvertor.class), fixture.fields, fixture.properties))
                    .withBean("roleController", RoleController.class,
                            () -> new RoleController(roles, new UserAdapterConvertorImpl(), fixture.fields, fixture.properties))
                    .withBean("permissionController", PermissionController.class,
                            () -> new PermissionController(permissions, new UserAdapterConvertorImpl(), fixture.fields, fixture.properties))
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        var mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
                        var entries = List.of(new String[]{"POST","/api/users","{\"externalId\":\"e\",\"name\":\"Reader\",\"email\":\"reader@example.test\"}"},
                                new String[]{"GET","/api/users/1001",""}, new String[]{"POST","/api/users/1001/roles","{\"roleCode\":\"teacher\"}"},
                                new String[]{"POST","/api/roles/teacher/permissions","{\"permissionCode\":\"course:read\"}"}, new String[]{"GET","/api/users/1001/permissions",""},
                                new String[]{"POST","/api/courses","{\"code\":\"MATH\",\"name\":\"Math\"}"}, new String[]{"GET","/api/courses/1002",""},
                                new String[]{"POST","/api/school-classes","{\"name\":\"Class One\",\"semester\":\"2026-FALL\"}"}, new String[]{"GET","/api/school-classes/1003",""},
                                new String[]{"POST","/api/school-classes/1003/courses/1002/schedule","{\"startsAt\":\"2026-10-13T01:00:00Z\",\"endsAt\":\"2026-10-13T02:00:00Z\"}"});
                        for (var entry : entries) {
                            mvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(entry[0]), entry[1])
                                    .contentType("application/json").content(entry[2])).andExpect(status().isUnauthorized());
                            mvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(entry[0]), entry[1])
                                    .header("Authorization", "Bearer "+fixture.token("1001",Set.of("openid"))).contentType("application/json").content(entry[2]))
                                    .andExpect(status().isForbidden());
                        }
                        assertThat(fixture.permissionChecks).isZero();
                        var intents = List.of("user:create", "user:read", "role:assign", "permission:grant", "permission:read", "course:create", "course:read", "school-class:create", "school-class:read", "school-class:schedule-course");
                        var grantedPermissions = Set.copyOf(fixture.permissions);
                        fixture.permissions.clear(); fixture.clearSnapshots();
                        for (int index = 0; index < entries.size(); index++) {
                            var entry = entries.get(index); String intent = SecurityFixture.SYSTEM+":"+intents.get(index);
                            mvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(entry[0]),entry[1])
                                    .header("Authorization","Bearer "+fixture.token("1001",Set.of(intent))).contentType("application/json").content(entry[2]))
                                    .andExpect(status().isForbidden());
                        }
                        fixture.permissions.addAll(grantedPermissions);
                        for (var access : List.of(FieldAccessLevel.READ,
                                FieldAccessLevel.MASKED_READ,FieldAccessLevel.NONE)) {
                            fixture.fieldLevel = access; fixture.clearSnapshots();
                            for (int index = 0; index < entries.size(); index++) {
                                var entry = entries.get(index); if (!"POST".equals(entry[0])) { continue; }
                                mvc.perform(MockMvcRequestBuilders.post(entry[1])
                                        .header("Authorization","Bearer "+fixture.token("1001",Set.of(SecurityFixture.SYSTEM+":"+intents.get(index)))).contentType("application/json").content(entry[2]))
                                        .andExpect(status().isForbidden())
                                        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("FIELD_ACCESS_DENIED"));
                            }
                        }
                        Mockito.verifyNoInteractions(users,courses,classes,roles,permissions);
                    });
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void tenActualProtobufMethodsUseExactTenantServicePoliciesAndRestoreCallbackProof(@TempDir Path temporary) throws Exception {
        try (var fixture = new SecurityFixture(temporary)) {
            var descriptors = List.of(
                    CourseServiceGrpc.getCreateCourseMethod(), CourseServiceGrpc.getGetCourseMethod(),
                    SchoolClassServiceGrpc.getCreateSchoolClassMethod(), SchoolClassServiceGrpc.getGetSchoolClassMethod(), SchoolClassServiceGrpc.getScheduleCourseMethod(),
                    UserServiceGrpc.getCreateUserMethod(), UserServiceGrpc.getGetUserMethod(), UserServiceGrpc.getAssignRoleMethod(),
                    PermissionServiceGrpc.getGrantPermissionMethod(), PermissionServiceGrpc.getGetUserPermissionsMethod());
            var values = new LinkedHashMap<String,Object>();
            for (var source : new YamlPropertySourceLoader().load("rpcMatrix", new ClassPathResource("application-test.yml"))) {
                ((MapPropertySource)source).getSource().forEach((key,value)-> {
                    if (key.startsWith("egon.cola.platform.tianquan.shoubing.rpc.provider.")) {
                        Object raw = value instanceof OriginTrackedValue tracked ? tracked.getValue() : value;
                        values.put(key,key.endsWith("expected-resource-uri") ? SecurityFixture.RESOURCE.toASCIIString() : raw);
                    }
                });
            }
            var configured = new Binder(new MapConfigurationPropertySource(values))
                    .bind("egon.cola.platform.tianquan.shoubing.rpc.provider",Bindable.of(IdpStarterProperties.RpcProviderProperties.class)).get();
            fixture.identityProperties.getRpc().setEnabled(true); configured.setEnabled(true); fixture.identityProperties.getRpc().setProvider(configured);
            var interceptor = IdpRpcBearerServerInterceptor.createConfigured(
                    new UserAccessTokenVerifier(fixture.verifier),fixture.verifier,fixture.identityProperties,fixture.observations,fixture.validator);
            for (MethodDescriptor descriptor : descriptors) {
                var policy = configured.getMethods().get(descriptor.getFullMethodName()); assertThat(policy).isNotNull();
                String granted = fixture.serviceToken("1001",policy.getRequiredScopes());
                String userToken = fixture.token("1001",policy.getRequiredScopes());
                for (String bearer : List.of("",userToken,fixture.serviceToken("1001",Set.of("unrelated")),granted)) {
                    ServerCall call = mock(ServerCall.class); when(call.getMethodDescriptor()).thenReturn(descriptor); when(call.getAttributes()).thenReturn(Attributes.EMPTY);
                    var status = new AtomicReference<Status>();
                    Mockito.doAnswer(invocation -> {status.set(invocation.getArgument(0));return null;}).when(call).close(ArgumentMatchers.any(),ArgumentMatchers.any());
                    var headers = new Metadata(); if (!bearer.isEmpty()) { headers.put(Metadata.Key.of("authorization",Metadata.ASCII_STRING_MARSHALLER),"Bearer "+bearer); }
                    var entered = new AtomicInteger();
                    var marker = new AtomicReference<IdpAuthorizationObservationContextBO>();
                    var previous = SecurityContextHolder.createEmptyContext(); SecurityContextHolder.setContext(previous);
                    MDC.put("tenantId","previous"); MDC.put("userId","previous-actor");
                    var listener = interceptor.interceptCall(call,headers,(server,incoming)-> {
                        entered.incrementAndGet();
                        assertThat(IdpRpcSecurityContext.currentPrincipalOrNull()).isInstanceOf(ServiceIdentityPrincipal.class);
                        assertThat(IdpRpcSecurityContext.currentTokenOrNull()).isNull();
                        assertThat(IdpRpcSecurityContext.currentProofOrNull()).isNotNull();
                        marker.set(IdpRpcSecurityContext.currentAuthorizationObservationContext());
                        assertThat(MDC.get("tenantId")).isEqualTo("1001"); assertThat(MDC.get("userId")).isEqualTo("light-service");
                        return new ServerCall.Listener() { @Override public void onHalfClose() { assertThat(MDC.get("tenantId")).isEqualTo("1001"); } };
                    });
                    if (bearer.equals(granted)) { assertThat(entered.get()).isEqualTo(1); listener.onHalfClose(); listener.onCancel(); assertThat(marker.get().isActive()).isFalse(); }
                    else { assertThat(entered.get()).isZero(); assertThat(status.get().getCode()).isEqualTo(bearer.isEmpty() ? Status.Code.UNAUTHENTICATED : Status.Code.PERMISSION_DENIED); }
                    assertThat(SecurityContextHolder.getContext()).isSameAs(previous); assertThat(MDC.get("tenantId")).isEqualTo("previous");
                    assertThat(IdpRpcSecurityContext.currentPrincipalOrNull()).isNull();
                    MDC.clear(); SecurityContextHolder.clearContext();
                }
            }
        }
    }

    @Test
    void verifiedServiceScopeReachesTheActualSpringProviderAndKeepsItsSuccessWire(@TempDir Path temporary) throws Exception {
        var users = mock(UserRepository.class);
        var courses = mock(CourseManage.class);
        when(courses.get(new GetCourseQuery(1002L))).thenReturn(new CourseResult(1002L, "MATH", "Math", "ACTIVE"));
        try (var fixture = new SecurityFixture(temporary)) {
            runner(fixture, users)
                    .withBean("serviceScopeAuthorization", ServiceScopeAuthorization.class, ServiceScopeAuthorization::new)
                    .withBean("egonColaValidationUtils", ValidationUtils.class, () -> new ValidationUtils(fixture.validator))
                    .withBean("idpObservedVerificationComponent", IdpObservedVerificationComponent.class, () -> fixture.observations)
                    .withUserConfiguration(NativeRpcConfiguration.class)
                    .withBean("courseFacadeImpl", CourseFacadeImpl.class,
                            () -> new CourseFacadeImpl(courses, Mappers.getMapper(LightFacadeConverter.class), new ValidationUtils(fixture.validator)))
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        String bearer = fixture.serviceToken("1001", Set.of(SecurityFixture.SYSTEM + ":course:read"));
                        var verified = (AccessTokenVerification.Valid<?>) fixture.verifier.verifyService(bearer);
                        var callback = IdpRpcSecurityContext.with(verified.principal(), bearer, verified.proof(),
                                new IdpAuthorizationObservationContextBO(), verified.scopes());
                        assertThatCode(() -> callback.run(() -> IdpRpcSecurityContext.runWithSpringSecurity(
                                verified.principal(), verified.scopes(), verified.proof(), () -> {
                                    var result = context.getBean(CourseFacade.class)
                                            .getCourse(GetCourseRpcRequest.newBuilder().setCourseId(1002L).build());
                                    assertThat(result.getSuccess()).isTrue();
                                    assertThat(result.getData().getCode()).isEqualTo("MATH");
                                }))).doesNotThrowAnyException();
                        verify(courses).get(new GetCourseQuery(1002L));
                    });
        }
    }

    @Test
    void allSixWriteSetsRequireExplicitRoleRulesAndRejectUnknownFields(@TempDir Path temporary) throws Exception {
        var writeSets = Map.of("user:create", Set.of("externalId", "name", "email"),
                "role:assign", Set.of("roleCode"), "permission:grant", Set.of("permissionCode"),
                "course:create", Set.of("code", "name"), "school-class:create", Set.of("name", "semester"),
                "school-class:schedule-course", Set.of("startsAt", "endsAt"));
        try (var fixture = new SecurityFixture(temporary)) {
            var previous = SecurityContextHolder.getContext();
            try {
                for (var entry : writeSets.entrySet()) {
                    String permission = SecurityFixture.SYSTEM + ":" + entry.getKey();
                    String resource = SecurityFixture.SYSTEM + ".api." + entry.getKey().replace(':', '.');
                    var command = new FieldWriteAuthorizationCommand(permission, SecurityFixture.SYSTEM, resource, entry.getValue());
                    SecurityContextHolder.getContext().setAuthentication(fixture.authenticatedUser("1001", Set.of(permission)));
                    assertThat(fixture.fields.requireWritableFields(command).fields().values())
                            .allSatisfy(access -> assertThat(access.level()).isEqualTo(FieldAccessLevel.WRITE));
                    assertThatThrownBy(() -> fixture.fields.requireWritableFields(
                            new FieldWriteAuthorizationCommand(permission, SecurityFixture.SYSTEM, resource, Set.of("unregistered"))))
                            .isInstanceOf(AccessDeniedException.class);
                    fixture.explicitFieldRules = false;
                    SecurityContextHolder.getContext().setAuthentication(fixture.authenticatedUser("1001", Set.of(permission)));
                    assertThatThrownBy(() -> fixture.fields.requireWritableFields(command))
                            .isInstanceOf(AccessDeniedException.class);
                    fixture.explicitFieldRules = true;
                }
            } finally {
                SecurityContextHolder.setContext(previous);
            }
        }
    }

    @Test
    void rpcMethodDenialConfirmsOneAuditAndClosedAuthorityIsUnavailable(@TempDir Path temporary) throws Exception {
        try (var fixture = new SecurityFixture(temporary)) {
            runner(fixture, mock(UserRepository.class))
                    .withBean("egonColaValidationUtils", ValidationUtils.class, () -> new ValidationUtils(fixture.validator))
                    .withBean("idpObservedVerificationComponent", IdpObservedVerificationComponent.class, () -> fixture.observations)
                    .withUserConfiguration(NativeRpcConfiguration.class)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        var mapper = context.getBean("nativeRpcValidationExceptionMapper", RpcProviderExceptionMapper.class);
                        String bearer = fixture.serviceToken("1001", Set.of(SecurityFixture.SYSTEM + ":course:read"));
                        var verified = (AccessTokenVerification.Valid<?>) fixture.verifier.verifyService(bearer);
                        var marker = new IdpAuthorizationObservationContextBO();
                        var callback = IdpRpcSecurityContext.with(verified.principal(), bearer, verified.proof(), marker, verified.scopes());
                        callback.run(() -> {
                            for (var denied : List.of(new AccessDeniedException("private reason"),
                                    new EgonRpcRejectedException("RPC actor conflicts with verified identity"))) {
                                var failure = mapper.map(denied).orElseThrow();
                                assertThat(failure.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
                                assertThat(failure.getStatus().getDescription()).isEqualTo("service_scope_denied");
                            }
                            assertThat(fixture.events.stream().filter(event -> "AUTHORIZATION_DENIED".equals(event.get("reasonCode"))).count()).isEqualTo(1);
                            marker.close();
                            assertThat(mapper.map(new AccessDeniedException("late denial")).orElseThrow().getStatus().getCode())
                                    .isEqualTo(Status.Code.UNAVAILABLE);
                        });
                        assertThat(IdpRpcSecurityContext.currentPrincipalOrNull()).isNull();
                    });
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ReadChainConfiguration {
        @Bean("controlledUserDetailsService")
        UserDetailsService noPasswordLogin() { return username -> { throw new UsernameNotFoundException("No password login"); }; }

        @Bean("userController")
        UserController userController(UserManage manage,
                Rbac3FieldWriteAuthorizationComponent fields,
                Rbac3StarterProperties properties) {
            return new UserController(manage, new UserAdapterConvertorImpl(), fields, properties);
        }

    }

    /** Keep the data port outside Spring so its inherited MP Mapper wiring cannot impersonate a configured database. */
    public static UserManage readManage(UserRepository repository) {
        var domain = new UserDomainServiceImpl(repository, mock(UserRoleRepository.class), new UserPOConverterImpl());
        return new UserManageImpl(domain, mock(UserQueryService.class), mock(UserEventService.class),
                mock(UserIdempotencyService.class), mock(UserApplicationValidator.class), new UserApplicationConvertorImpl());
    }
    public static WebApplicationContextRunner runner(SecurityFixture fixture, UserRepository repository) {
        return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
                WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,JacksonAutoConfiguration.class,ValidationAutoConfiguration.class))
                .withUserConfiguration(NativeHttpSecurityConfiguration.class, ReadChainConfiguration.class)
                .withBean("userManageImpl", UserManage.class, () -> readManage(repository))
                .withBean("domainRequestContextFilter", RequestContextFilter.class, () -> new RequestContextFilter(fixture.mapper, fixture.persistenceProperties))
                .withBean("idpBearerAuthenticationFilter", IdpBearerAuthenticationFilter.class, () -> fixture.identityFilter)
                .withBean("idpSecurityObservationFilter", IdpSecurityObservationFilter.class, () -> fixture.observationFilter)
                .withBean("rbac3BearerAuthenticationFilter", Rbac3BearerAuthenticationFilter.class, () -> fixture.rbacFilter)
                .withBean("rbac3FieldWriteAuthorizationComponent", Rbac3FieldWriteAuthorizationComponent.class, () -> fixture.fields)
                .withBean("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties",Rbac3StarterProperties.class, () -> fixture.properties)
                .withBean("rbac3MethodAuthorizationInterceptor", AuthorizationManagerBeforeMethodInterceptor.class, () -> fixture.advisor,
                        definition -> definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE));
    }
    /** Shared explicit test composition; production auto-configuration remains disabled in the external-free test profile. */
    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    public static class SecurityTestBeans {
        @Bean(name = "nativeSecurityFixture", destroyMethod = "close")
        SecurityFixture fixture() throws Exception { return new SecurityFixture(Files.createTempDirectory("light-security-test-")); }
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
        @Bean("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
        @ConfigurationProperties(prefix = "egon.cola.component.mybatis-plus")
        EgonColaMybatisPlusProperties persistenceProperties(SecurityFixture fixture) { return fixture.persistenceProperties; }
        @Bean("rbac3MethodAuthorizationInterceptor") @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        AuthorizationManagerBeforeMethodInterceptor advisor(SecurityFixture fixture) { return fixture.advisor; }
        @Bean("authorizationService") AuthorizationService authorization(SecurityFixture fixture) { return fixture.authorization; }
        @Bean("rbac3AuthorizationExceptionHandler") Rbac3AuthorizationExceptionHandler errors() {
            return new Rbac3AuthorizationExceptionHandler();
        }
    }

    /** Real multi-issuer verifier, audit producer, snapshot cache, PDP and field guard; only remote stores are controlled. */
    public static final class SecurityFixture implements AutoCloseable {
        public static final String SYSTEM = "egon-cola-source-light";
        public static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");
        public static final URI RESOURCE = URI.create("https://light.example.test");
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
            settings.setResourceServerId("light-resource"); settings.setResourceUri(SecurityFixture.RESOURCE);
            settings.getRevocation().setPrimaryEndpoint("rediss://redis.example.test:6379"); settings.getRevocation().setEpoch("fixture-epoch");
            var issuers = new LinkedHashMap<String,IdpStarterProperties.TrustedIssuerSettings>();
            for (String tenant : List.of("1001", "1002")) {
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
            for (String tenant : List.of("1001", "1002")) {
                jwks.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo("https://auth.example.test/tenants/"+tenant+"/.well-known/jwks.json"))
                        .andRespond(MockRestResponseCreators.withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
            }
            when(revocations.check(ArgumentMatchers.any())).thenReturn(OAuthRevocationReader.Result.valid());
            var resourceStates = mock(IdentityResourceServerStateReader.class);
            when(resourceStates.read("light-resource")).thenReturn(Optional.of(new IdentityResourceServerState(
                    "light-resource", SecurityFixture.RESOURCE, "sample", SecurityFixture.SYSTEM, "test", ResourceServerStatus.ACTIVE, 7)));
            verifier = IdpJwtVerifier.createConfigured(null, resourceStates,
                    mock(IdentityOAuthClientStateReader.class), "light-resource", SecurityFixture.RESOURCE, "platform", clock,
                    managers, revocations, Mappers.getMapper(VerifiedPrincipalConverter.class), null, observations);
            identityFilter = IdpBearerAuthenticationFilter.create(
                    new UserAccessTokenVerifier(verifier),
                    new ServiceAccessTokenVerifier(verifier), endpointPolicy, mapper,
                    new VerifiedTenantContextFilter(), verifier);
            observationFilter = new IdpSecurityObservationFilter(observations, endpointPolicy);
            properties.setEnabled(true); properties.setRegisterFilter(false); properties.setSystemCode(SecurityFixture.SYSTEM); properties.getUserScopePolicy().setEnabled(true);
            var policy = new LinkedHashMap<String,Set<String>>();
            for (String intent : List.of("user:create", "user:read", "role:assign", "permission:grant", "permission:read", "course:create", "course:read", "school-class:create", "school-class:read", "school-class:schedule-course")) {
                String permission = SecurityFixture.SYSTEM+":"+intent; policy.put(permission, Set.of(permission)); permissions.add(permission);
            }
            properties.getUserScopePolicy().setPermissionScopes(policy);
            for (String resource : List.of("api.user.create", "api.user.read", "api.role.assign", "api.permission.grant", "api.permission.read", "api.course.create", "api.course.read", "api.school-class.create", "api.school-class.read", "api.school-class.schedule-course", "graphql.query.user", "graphql.query.permissions", "graphql.query.course", "graphql.query.schoolClass")) { resources.add(SecurityFixture.SYSTEM+"."+resource); }
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
            var writeSets = Map.of("user:create", Set.of("externalId","name","email"), "role:assign", Set.of("roleCode"),
                    "permission:grant", Set.of("permissionCode"), "course:create", Set.of("code","name"),
                    "school-class:create", Set.of("name","semester"), "school-class:schedule-course", Set.of("startsAt","endsAt"));
            writeSets.forEach((intent,names)-> {
                String permission = SecurityFixture.SYSTEM+":"+intent; String resource = SecurityFixture.SYSTEM+".api."+intent.replace(':','.');
                var facts = names.stream().map(name -> new AuthorizationRuleFacts.FieldRuleFact("501",resource,name,fieldLevel)).toList();
                if (!explicitFieldRules) { facts = List.of(); }
                var caps = names.stream().map(name -> new AuthorizationRuleFacts.FieldDefinitionFact(resource,name,FieldAccessLevel.WRITE)).toList();
                var merged = new FieldPolicyMerger().merge(facts,caps,Set.of("501"));
                var fields = new LinkedHashMap<String,FieldPolicyDecision.FieldAccess>();
                names.forEach(name -> fields.put(name,new FieldPolicyDecision.FieldAccess(
                        merged.getOrDefault(resource+"#"+name, FieldAccessLevel.NONE),null)));
                var decision = new FieldPolicyDecision(Decision.ALLOW,"ALLOW",
                        principal.tenantId(),"2001",permission,SecurityFixture.SYSTEM,resource,fields,1,2,List.of("field-rule-501"),NOW);
                fieldPolicies.put(permission+":"+SecurityFixture.SYSTEM+":"+resource,decision);
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
            claims.put("iss","https://auth.example.test/tenants/"+tenant); claims.put("sub",service ? "light-service" : "subject-"+tenant); claims.put("aud",List.of(SecurityFixture.RESOURCE.toASCIIString()));
            claims.put("iat",NOW.minusSeconds(1).getEpochSecond()); claims.put("nbf",NOW.minusSeconds(1).getEpochSecond()); claims.put("exp",NOW.plusSeconds(300).getEpochSecond());
            claims.put("jti",Long.toString(tokenIds.incrementAndGet())); claims.put("tenant_id",tenant); claims.put("client_id",service ? "light-service" : "browser-client"); claims.put("principal_type",service ? "SERVICE" : "USER");
            claims.put("scope_context","TENANT"); claims.put("authorization_id","801"); claims.put("client_registration_id","501");
            claims.put("security_version",service ? Map.of("realm",1L,"client",2L) : Map.of("realm",1L,"client",2L,"subject",3L)); claims.put("scope",List.copyOf(scopes));
            if (service) {
                claims.put("resource_version",7L); claims.put("source_biz","sample"); claims.put("source_app",SecurityFixture.SYSTEM); claims.put("source_env","test");
                claims.put("credential_id","credential-1"); claims.put("app_id",SecurityFixture.SYSTEM);
            } else { claims.put("acr","PASSWORD"); claims.put("auth_time",NOW.minusSeconds(1).getEpochSecond()); }
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("same-kid").type(new JOSEObjectType("at+jwt")).build(),JWTClaimsSet.parse(claims));
            jwt.sign(new RSASSASigner(key)); return jwt.serialize();
        }
        @Override public void close() { validator.close(); SecurityContextHolder.clearContext(); MDC.clear(); }
    }

}
