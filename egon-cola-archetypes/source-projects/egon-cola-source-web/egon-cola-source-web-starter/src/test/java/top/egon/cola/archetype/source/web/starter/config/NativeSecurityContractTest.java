package top.egon.cola.archetype.source.web.starter.config;

import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironmentImpl;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.ServletException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Mono;
import top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityInterceptor;
import top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityInstrumentation;

import top.egon.cola.archetype.source.web.adapter.teaching.controller.GradeController;
import top.egon.cola.archetype.source.web.adapter.teaching.controller.SchoolClassController;
import top.egon.cola.archetype.source.web.adapter.teaching.facade.impl.GradeFacadeImpl;
import top.egon.cola.archetype.source.web.adapter.teaching.facade.impl.SchoolClassFacadeImpl;
import top.egon.cola.archetype.source.web.adapter.teaching.graphql.SchoolClassResolver;
import top.egon.cola.archetype.source.web.adapter.user.controller.PermissionController;
import top.egon.cola.archetype.source.web.adapter.user.controller.RoleController;
import top.egon.cola.archetype.source.web.adapter.user.facade.impl.PermissionFacadeImpl;
import top.egon.cola.archetype.source.web.adapter.user.facade.impl.RoleFacadeImpl;
import top.egon.cola.archetype.source.web.adapter.user.facade.impl.UserFacadeImpl;
import top.egon.cola.archetype.source.web.adapter.user.graphql.UserResolver;
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
import org.junit.jupiter.api.io.TempDir;
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
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.test.context.TestConfiguration;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
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
import jakarta.servlet.Filter;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.egon.cola.archetype.source.web.adapter.user.controller.UserController;
import top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.UserAdapterConverter;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.manage.impl.UserManageImpl;
import top.egon.cola.archetype.source.web.application.user.pojo.convertor.UserConverter;
import top.egon.cola.archetype.source.web.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.web.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.web.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.web.infrastructure.user.converter.RolePOConverter;
import top.egon.cola.archetype.source.web.infrastructure.user.converter.UserPOConverter;
import top.egon.cola.archetype.source.web.infrastructure.user.po.UserPO;
import top.egon.cola.archetype.source.web.infrastructure.user.repo.PermissionRepository;
import top.egon.cola.archetype.source.web.infrastructure.user.repo.RolePermissionRepository;
import top.egon.cola.archetype.source.web.infrastructure.user.repo.RoleRepository;
import top.egon.cola.archetype.source.web.infrastructure.user.repo.UserRepository;
import top.egon.cola.archetype.source.web.infrastructure.user.repo.UserRoleRepository;
import top.egon.cola.archetype.source.web.infrastructure.user.service.impl.UserDomainServiceImpl;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Actual host/controller/application/domain conversion; only data and unrelated event ports are controlled. */
public class NativeSecurityContractTest {
    @Test
    void missingBearerStopsTheActualUserReadBeforeItsDataPort(@TempDir Path temporary) throws Exception {
        var users = mock(UserRepository.class);
        try (var security = new SecurityFixture(temporary)) {
            hostRunner(users, security).run(context -> {
                assertThat(context).hasNotFailed();
                MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build()
                        .perform(get("/api/v1/users/1001")).andExpect(status().isUnauthorized());
                verifyNoInteractions(users);
            });
        }
    }

    @Test
    void signedHttpRequiresBothScopeAndPdpAndIgnoresIdentityHeaders(@TempDir Path temporary) throws Exception {
        var users = mock(UserRepository.class);
        var row = UserPO.builder().name("Reader").email("reader@example.test").status("ACTIVE").build(); row.setId(1001L);
        when(users.findCachedById(1001L)).thenAnswer(invocation -> {
            assertThat(MDC.get("tenantId")).isEqualTo("1002");
            assertThat(MDC.get("userId")).isEqualTo("subject-1002");
            return row;
        });
        try (var security = new SecurityFixture(temporary)) {
            hostRunner(users, security).run(context -> {
                assertThat(context).hasNotFailed();
                var mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
                mvc.perform(get("/api/v1/users/1001").header("Authorization", "Bearer " + security.token("1001", Set.of("openid"))))
                        .andExpect(status().isForbidden());
                assertThat(security.permissionChecks).isZero(); verifyNoInteractions(users);
                mvc.perform(get("/api/v1/users/1001").header("Authorization", "Bearer " + security.token("1002", Set.of(SecurityFixture.SYSTEM + ":user:read")))
                                .header("X-Tenant-Id", "9999").header("X-Actor-Id", "forged").header("X-Actor-Roles", "SYSTEM"))
                        .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.name").value("Reader"));
                assertThat(security.permissionChecks).isGreaterThan(0);
                assertThat(MDC.get("tenantId")).isNull(); assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            });
        }
    }

    static UserManage realReadManage(UserRepository users, SecurityFixture security) {
        var domain = new UserDomainServiceImpl(users, mock(UserRoleRepository.class), mock(RoleRepository.class),
                mock(RolePermissionRepository.class), mock(PermissionRepository.class),
                Mappers.getMapper(UserPOConverter.class), Mappers.getMapper(RolePOConverter.class));
        return new UserManageImpl(domain, new UserApplicationValidator(new ValidationUtils(security.validator)),
                Mappers.getMapper(UserConverter.class), mock(CommandIdempotencyService.class), mock(OrganizationEventService.class));
    }

    @Test
    void signedHttpGraphQlCapturesVerifiedScopeAndRestoresItForTheRealResolver(@TempDir Path temporary) throws Exception {
        var users = mock(UserRepository.class);
        var row = UserPO.builder().name("Reader").email("reader@example.test").status("ACTIVE").build(); row.setId(1001L);
        when(users.findCachedById(1001L)).thenReturn(row);
        try (var security = new SecurityFixture(temporary)) {
            hostRunner(users, security)
                    .withConfiguration(AutoConfigurations.of(org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration.class,
                            org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration.class))
                    .withUserConfiguration(top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityInterceptor.class,
                            top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityInstrumentation.class,
                            top.egon.cola.archetype.source.web.adapter.handler.OrganizationGraphQlExceptionResolver.class)
                    .withBean("defaultValidator", jakarta.validation.Validator.class, () -> security.validator)
                    .withBean("userResolver", UserResolver.class, () -> new UserResolver(security.fields, security.properties,
                            realReadManage(users, security), mock(top.egon.cola.archetype.source.web.application.user.manage.RoleManage.class),
                            mock(top.egon.cola.archetype.source.web.application.user.manage.PermissionManage.class)))
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        var mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
                        String query = security.mapper.writeValueAsString(Map.of("operationName", "NativeWebUser", "query",
                                "query NativeWebUser($id:ID!){user(id:$id){id name email status roleCodes}}", "variables", Map.of("id", "1001")));
                        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/graphql").contentType("application/json").content(query)
                                        .header("Authorization", "Bearer " + security.token("1001", Set.of("openid"))))
                                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION_DENIED"));
                        verifyNoInteractions(users);
                        long deniedBefore = security.events.stream().filter(event -> "AUTHORIZATION_DENIED".equals(event.get("reasonCode"))).count();
                        String aliases = security.mapper.writeValueAsString(Map.of("operationName", "TwoDenied", "query",
                                "query TwoDenied($id:ID!){left:user(id:$id){id} right:user(id:$id){id}}", "variables", Map.of("id", "1001")));
                        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/graphql").contentType("application/json").content(aliases)
                                        .header("Authorization", "Bearer " + security.token("1001", Set.of("openid"))))
                                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.errors.length()").value(2));
                        assertThat(security.events.stream().filter(event -> "AUTHORIZATION_DENIED".equals(event.get("reasonCode"))).count()).isEqualTo(deniedBefore + 1);
                        verifyNoInteractions(users);
                        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/graphql").contentType("application/json").content(query)
                                        .header("Authorization", "Bearer " + security.token("1001", Set.of(SecurityFixture.SYSTEM + ":user:read"))))
                                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.user.name").value("Reader"));
                        assertThat(top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder.current()).isEmpty();
                        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                    });
        }
    }

    static WebApplicationContextRunner hostRunner(UserRepository users, SecurityFixture security) {
        return new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
                        SecurityFilterAutoConfiguration.class, WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
                        HttpMessageConvertersAutoConfiguration.class))
                .withUserConfiguration(NativeHttpSecurityConfiguration.class,
                        top.egon.cola.archetype.source.web.adapter.handler.OrganizationGlobalExceptionHandler.class)
                .withBean("userManage", UserManage.class, () -> realReadManage(users, security))
                .withBean("userController", UserController.class,
                        () -> new UserController(security.fields, security.properties, realReadManage(users, security), Mappers.getMapper(UserAdapterConverter.class)))
                .withBean("organizationAuthContextFilter", top.egon.cola.archetype.source.web.adapter.filter.OrganizationAuthContextFilter.class,
                        () -> new top.egon.cola.archetype.source.web.adapter.filter.OrganizationAuthContextFilter(security.mapper, "tenantId", "userId"))
                .withBean("idpBearerAuthenticationFilter", IdpBearerAuthenticationFilter.class, () -> security.identityFilter)
                .withBean("idpSecurityObservationFilter", IdpSecurityObservationFilter.class, () -> security.observationFilter)
                .withBean("rbac3BearerAuthenticationFilter", Rbac3BearerAuthenticationFilter.class, () -> security.rbacFilter)
                .withBean("rbac3AuthorizationExceptionHandler", Rbac3AuthorizationExceptionHandler.class, Rbac3AuthorizationExceptionHandler::new)
                .withBean("rbac3FieldWriteAuthorizationComponent", Rbac3FieldWriteAuthorizationComponent.class, () -> security.fields)
                .withBean("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties", Rbac3StarterProperties.class, () -> security.properties)
                .withBean("rbac3MethodAuthorizationInterceptor", AuthorizationManagerBeforeMethodInterceptor.class, () -> security.advisor,
                        definition -> definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE));
    }

    static WebApplicationContextRunner graphRunner(UserRepository users, SecurityFixture security) {
        return hostRunner(users, security)
                .withConfiguration(AutoConfigurations.of(org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration.class,
                        org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration.class))
                .withUserConfiguration(top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityInterceptor.class,
                        top.egon.cola.archetype.source.web.adapter.graphql.NativeGraphQlSecurityInstrumentation.class,
                        top.egon.cola.archetype.source.web.adapter.handler.OrganizationGraphQlExceptionResolver.class)
                .withBean("defaultValidator", jakarta.validation.Validator.class, () -> security.validator)
                .withBean("userResolver", UserResolver.class, () -> new UserResolver(security.fields, security.properties,
                        realReadManage(users, security), mock(top.egon.cola.archetype.source.web.application.user.manage.RoleManage.class),
                        mock(top.egon.cola.archetype.source.web.application.user.manage.PermissionManage.class)));
    }

    @Test
    void everyRestAndGraphQlWriteSetRequiresExplicitWriteRules(@TempDir Path temporary) throws Exception {
        var sets = Map.of("user:create", Set.of("name", "email"), "role:assign", Set.of("roleCode"),
                "permission:grant", Set.of("permissionCode"), "grade:create", Set.of("code", "name"),
                "school-class:create", Set.of("name", "gradeCode"), "school-class:assign-user", Set.of("userId"));
        var roots = Map.of("user:create", "createUser", "role:assign", "assignRole", "permission:grant", "grantPermission",
                "grade:create", "createGrade", "school-class:create", "createSchoolClass", "school-class:assign-user", "assignUserToSchoolClass");
        try (var security = new SecurityFixture(temporary)) {
            for (var entry : sets.entrySet()) {
                String permission = SecurityFixture.SYSTEM + ":" + entry.getKey();
                for (String suffix : List.of("api." + entry.getKey().replace(':', '.'), "graphql.mutation." + roots.get(entry.getKey()))) {
                    String resource = SecurityFixture.SYSTEM + "." + suffix;
                    var command = new FieldWriteAuthorizationCommand(permission, SecurityFixture.SYSTEM, resource, entry.getValue());
                    security.fieldLevel = FieldAccessLevel.WRITE;
                    security.explicitFieldRules = true;
                    SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", Set.of(permission)));
                    assertThat(security.fields.requireWritableFields(command).fields().values())
                            .allSatisfy(access -> assertThat(access.level()).isEqualTo(FieldAccessLevel.WRITE));
                    for (var level : List.of(FieldAccessLevel.READ, FieldAccessLevel.MASKED_READ, FieldAccessLevel.NONE)) {
                        security.fieldLevel = level;
                        SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", Set.of(permission)));
                        org.assertj.core.api.Assertions.assertThatThrownBy(() -> security.fields.requireWritableFields(command))
                                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
                    }
                    security.fieldLevel = FieldAccessLevel.WRITE; security.explicitFieldRules = false;
                    SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", Set.of(permission)));
                    org.assertj.core.api.Assertions.assertThatThrownBy(() -> security.fields.requireWritableFields(command))
                            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
                }
            }
        }
    }

    @Test
    void allFourProfilesBindTenRequiredProvidersAndThreeFixedReads() throws Exception {
        Set<String> baseline = null;
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            for (String profile : List.of("application.yml", "application-dev.yml", "application-prod.yml", "application-test.yml")) {
                var values = new LinkedHashMap<String, Object>();
                for (var source : new YamlPropertySourceLoader().load(profile, new ClassPathResource(profile))) {
                    ((MapPropertySource) source).getSource().forEach((key, value) -> values.put(key,
                            value instanceof OriginTrackedValue tracked ? tracked.getValue() : value));
                }
                var keys = values.keySet().stream().filter(key -> key.startsWith("egon.cola.platform.tianquan.")
                                || key.startsWith("spring.security.oauth2.client.") || key.startsWith("server.ssl."))
                        .filter(key -> !key.contains("tenant-registration-ids")).collect(java.util.stream.Collectors.toSet());
                if (baseline == null) { baseline = keys; } else { assertThat(keys).as(profile).isEqualTo(baseline); }
                values.put("TIANQUAN_SHOUBING_RESOURCE_URI", SecurityFixture.RESOURCE.toASCIIString());
                values.put("EVALUATION_RESOURCE_URI", "https://evaluation.example.test");
                values.put("spring.application.name", "organization-app"); values.put("APP_ENV", "test");
                values.put("organization.integrations.evaluation.application-namespace", "evaluation-app");
                values.put("organization.integrations.evaluation.course-group", "course");
                values.put("organization.integrations.evaluation.exam-group", "exam");
                values.put("organization.integrations.evaluation.score-group", "score");
                values.put("organization.integrations.evaluation.version", "1.0.0");
                for (String key : List.of("evaluation-course-read", "evaluation-exam-read", "evaluation-score-read")) {
                    values.put("egon.cola.platform.tianquan.shoubing.rpc.consumer.bindings." + key + ".tenant-registration-ids[1001]", "native-tenant-one");
                }
                var environment = new org.springframework.core.env.StandardEnvironment();
                environment.getPropertySources().addFirst(new MapPropertySource("contract", values));
                var rpc = Binder.get(environment).bind("egon.cola.platform.tianquan.shoubing.rpc", IdpStarterProperties.RpcProperties.class).get();
                assertThat(validators.getValidator().validate(rpc)).as(profile).isEmpty();
                assertThat(rpc.getProvider().getMethods()).hasSize(10);
                rpc.getProvider().getMethods().values().forEach(policy -> {
                    assertThat(policy.isRequired()).isTrue();
                    assertThat(policy.getPrincipalKind()).isEqualTo(IdpStarterProperties.RpcPrincipalKindEnum.SERVICE);
                    assertThat(policy.getScopeContext()).isEqualTo(top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT);
                    assertThat(policy.getExpectedResourceUri()).isEqualTo(SecurityFixture.RESOURCE);
                    assertThat(policy.getRequiredScopes()).hasSize(1).allSatisfy(scope -> assertThat(scope).startsWith(SecurityFixture.SYSTEM + ":"));
                });
                assertThat(rpc.getConsumer().getBindings()).hasSize(3);
                rpc.getConsumer().getBindings().values().forEach(binding -> {
                    assertThat(binding.getMode()).isEqualTo(IdpStarterProperties.RpcConsumerModeEnum.SERVICE);
                    assertThat(binding.getRegistrationId()).isNull();
                    assertThat(binding.getScopes()).hasSize(1).allSatisfy(scope -> assertThat(scope).startsWith("evaluation-app:"));
                    assertThat(binding.getProcessIdentity()).containsEntry("applicationName", "organization-app").containsEntry("env", "test");
                });
                var policy = Binder.get(environment).bind("egon.cola.platform.tianquan.jianshen.user-scope-policy", Rbac3StarterProperties.UserScopePolicyProperties.class).get();
                assertThat(validators.getValidator().validate(policy)).isEmpty();
                assertThat(policy.getPermissionScopes()).hasSize(10);
                policy.getPermissionScopes().forEach((permission, scopes) -> assertThat(scopes).containsExactly(permission));
            }
        }
    }

    @Test
    void allThirtyEntryProxiesRequireDelegatedScopeBeforeAnyUseCase(@TempDir Path temporary) throws Exception {
        try (var security = new SecurityFixture(temporary)) {
            new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                    .withUserConfiguration(EntryMethodSecurityConfiguration.class, GradeController.class, SchoolClassController.class, GradeFacadeImpl.class, SchoolClassFacadeImpl.class, SchoolClassResolver.class, PermissionController.class, RoleController.class, UserController.class, PermissionFacadeImpl.class, RoleFacadeImpl.class, UserFacadeImpl.class, UserResolver.class)
                    .withBean("rbac3FieldWriteAuthorizationComponent", Rbac3FieldWriteAuthorizationComponent.class, () -> security.fields)
                    .withBean("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties", Rbac3StarterProperties.class, () -> security.properties)
                    .withBean("rbac3MethodAuthorizationInterceptor", AuthorizationManagerBeforeMethodInterceptor.class, () -> security.advisor,
                            definition -> definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE))
                    .withBean("gradeManage", top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage.class, () -> mock(top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage.class))
                    .withBean("gradeAdapterConverterImpl", top.egon.cola.archetype.source.web.adapter.teaching.pojo.convertor.GradeAdapterConverter.class, () -> Mappers.getMapper(top.egon.cola.archetype.source.web.adapter.teaching.pojo.convertor.GradeAdapterConverter.class))
                    .withBean("schoolClassManage", top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage.class, () -> mock(top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage.class))
                    .withBean("schoolClassAdapterConverterImpl", top.egon.cola.archetype.source.web.adapter.teaching.pojo.convertor.SchoolClassAdapterConverter.class, () -> Mappers.getMapper(top.egon.cola.archetype.source.web.adapter.teaching.pojo.convertor.SchoolClassAdapterConverter.class))
                    .withBean("organizationFacadeConverter", top.egon.cola.archetype.source.web.adapter.pojo.convertor.OrganizationFacadeConverter.class, () -> Mappers.getMapper(top.egon.cola.archetype.source.web.adapter.pojo.convertor.OrganizationFacadeConverter.class))
                    .withBean("nativeRpcValidation", top.egon.cola.component.common.core.validation.ValidationUtils.class, () -> new ValidationUtils(security.validator))
                    .withBean("permissionManage", top.egon.cola.archetype.source.web.application.user.manage.PermissionManage.class, () -> mock(top.egon.cola.archetype.source.web.application.user.manage.PermissionManage.class))
                    .withBean("permissionAdapterConverterImpl", top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.PermissionAdapterConverter.class, () -> Mappers.getMapper(top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.PermissionAdapterConverter.class))
                    .withBean("roleManage", top.egon.cola.archetype.source.web.application.user.manage.RoleManage.class, () -> mock(top.egon.cola.archetype.source.web.application.user.manage.RoleManage.class))
                    .withBean("roleAdapterConverterImpl", top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.RoleAdapterConverter.class, () -> Mappers.getMapper(top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.RoleAdapterConverter.class))
                    .withBean("userManage", top.egon.cola.archetype.source.web.application.user.manage.UserManage.class, () -> mock(top.egon.cola.archetype.source.web.application.user.manage.UserManage.class))
                    .withBean("userAdapterConverterImpl", top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.UserAdapterConverter.class, () -> Mappers.getMapper(top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.UserAdapterConverter.class))
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", Set.of("openid")));
                        int entries = 0;
                        for (Class<?> type : List.of(GradeController.class, SchoolClassController.class, GradeFacadeImpl.class, SchoolClassFacadeImpl.class, SchoolClassResolver.class, PermissionController.class, RoleController.class, UserController.class, PermissionFacadeImpl.class, RoleFacadeImpl.class, UserFacadeImpl.class, UserResolver.class)) {
                            Object proxy = context.getBean(type);
                            for (var method : type.getDeclaredMethods()) {
                                if (!method.isAnnotationPresent(org.springframework.security.access.prepost.PreAuthorize.class)
                                        && !method.isAnnotationPresent(top.egon.cola.platform.tianquan.shoubing.starter.security.RequiresServiceScope.class)) { continue; }
                                Object[] arguments = new Object[method.getParameterCount()];
                                var parameterTypes = method.getParameterTypes();
                                for (int i = 0; i < arguments.length; i++) {
                                    if (parameterTypes[i] == String.class) { arguments[i] = "1001"; }
                                }
                                org.assertj.core.api.Assertions.assertThatThrownBy(() -> method.invoke(proxy, arguments))
                                        .as(type.getSimpleName() + "." + method.getName())
                                        .hasRootCauseInstanceOf(org.springframework.security.access.AccessDeniedException.class);
                                entries++;
                            }
                        }
                        assertThat(entries).isEqualTo(30);
                        assertThat(security.permissionChecks).as("delegated scope must precede PDP").isZero();
                        Set<String> delegatedScopes = Set.copyOf(security.permissions);
                        security.permissions.clear();
                        SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", delegatedScopes));
                        int fineGrainedEntries = 0;
                        for (Class<?> type : List.of(GradeController.class, SchoolClassController.class, SchoolClassResolver.class,
                                PermissionController.class, RoleController.class, UserController.class, UserResolver.class)) {
                            Object proxy = context.getBean(type);
                            for (var method : type.getDeclaredMethods()) {
                                if (!method.isAnnotationPresent(org.springframework.security.access.prepost.PreAuthorize.class)) { continue; }
                                Object[] arguments = new Object[method.getParameterCount()];
                                var parameterTypes = method.getParameterTypes();
                                for (int i = 0; i < arguments.length; i++) {
                                    if (parameterTypes[i] == String.class) { arguments[i] = "1001"; }
                                }
                                Throwable rejected = org.assertj.core.api.Assertions.catchThrowable(() -> method.invoke(proxy, arguments));
                                assertThat(rejected).as(type.getSimpleName() + "." + method.getName()).isInstanceOf(java.lang.reflect.InvocationTargetException.class);
                                assertThat(rejected.getCause()).isInstanceOfAny(org.springframework.security.access.AccessDeniedException.class,
                                        DefaultAuthorizationService.AuthorizationDeniedException.class);
                                fineGrainedEntries++;
                            }
                        }
                        assertThat(fineGrainedEntries).isEqualTo(20);
                        assertThat(security.permissionChecks).isGreaterThan(0);
                        Mockito.verifyNoInteractions(context.getBean("gradeManage"));
                        Mockito.verifyNoInteractions(context.getBean("schoolClassManage"));
                        Mockito.verifyNoInteractions(context.getBean("permissionManage"));
                        Mockito.verifyNoInteractions(context.getBean("roleManage"));
                        Mockito.verifyNoInteractions(context.getBean("userManage"));
                    });
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity(proxyTargetClass = true)
    static class EntryMethodSecurityConfiguration {
        @Bean("annotationTemplateExpressionDefaults")
        static org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults templates() {
            return new org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults();
        }
        @Bean("serviceScopeAuthorization")
        top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceScopeAuthorization scopes() {
            return new top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceScopeAuthorization();
        }
    }

    @Test
    void twoConcurrentDelayedCallbacksRestoreExpiredDispatchAttributesAndRecordOneDenial(@TempDir Path temporary) throws Exception {
        var repository = mock(UserRepository.class);
        try (var fixture = new SecurityFixture(temporary)) {
            graphRunner(repository,fixture).run(context -> {
                assertThat(context).hasNotFailed();
                var request = new MockHttpServletRequest("POST","/graphql");
                var response = new MockHttpServletResponse();
                var captured = new AtomicReference<GraphQLContext>();
                SecurityContextHolder.getContext().setAuthentication(fixture.authenticatedUser("1001", Set.of("openid")));
                top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder.set(new top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext("subject-1001",Set.of(),"trace-1"));
                MDC.setContextMap(Map.of("tenantId","1001","userId","subject-1001","traceId","trace-1"));
                var dispatchAttributes = new ServletRequestAttributes(request,response);
                org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(dispatchAttributes);
                fixture.observationFilter.doFilter(request,response,(incoming,outgoing)-> {
                    var webRequest = new WebGraphQlRequest(URI.create("https://organization.example.test/graphql"),
                            new HttpHeaders(),new LinkedMultiValueMap<>(),Map.of(),
                            Map.of("query","query TwoAliases { left: user(id: \"1001\") { id } right: user(id: \"1001\") { id } }"),"request-1",Locale.ENGLISH);
                    context.getBean(NativeGraphQlSecurityInterceptor.class).intercept(webRequest,next -> {
                        captured.set(next.toExecutionInput().getGraphQLContext()); return Mono.empty();
                    });
                    dispatchAttributes.requestCompleted();
                    var environment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().graphQLContext(captured.get())
                            .arguments(Map.of("id","1001")).build();
                    DataFetcher<?> delegate = env -> context.getBean(UserResolver.class).user(env.getArgument("id"));
                    var fetcher = context.getBean(NativeGraphQlSecurityInstrumentation.class).instrumentDataFetcher(delegate,null,null);
                    var ready = new CountDownLatch(2); var release = new CountDownLatch(1);
                    try (var workers = Executors.newFixedThreadPool(2)) {
                        var results = new ArrayList<Future<?>>();
                        for (int index=0;index<2;index++) {
                            int workerId=index;
                            results.add(workers.submit(()-> {
                                var previousSecurity=SecurityContextHolder.createEmptyContext();
                                SecurityContextHolder.setContext(previousSecurity);
                                var previousBusiness=new top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext("worker-"+workerId,Set.of(),"previous");
                                top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder.set(previousBusiness);
                                MDC.setContextMap(Map.of("tenantId","77","worker","worker-"+workerId)); var previousMdc=MDC.getCopyOfContextMap();
                                var previousAttributes=new ServletRequestAttributes(new MockHttpServletRequest()); previousAttributes.requestCompleted();
                                org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(previousAttributes);
                                ready.countDown();
                                try {
                                    assertThat(release.await(5,TimeUnit.SECONDS)).isTrue();
                                    Assertions.assertThatThrownBy(()->fetcher.get(environment)).isInstanceOf(AccessDeniedException.class);
                                    assertThat(SecurityContextHolder.getContext()).isSameAs(previousSecurity);
                                    assertThat(top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder.current().orElseThrow()).isSameAs(previousBusiness);
                                    assertThat(org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).isSameAs(previousAttributes);
                                    assertThat(MDC.getCopyOfContextMap()).isEqualTo(previousMdc);
                                } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
                                finally { SecurityContextHolder.clearContext(); top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder.clear(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); MDC.clear(); }
                            }));
                        }
                        try {
                            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue(); release.countDown();
                            for (var result:results) { result.get(5,TimeUnit.SECONDS); }
                        } catch (Exception failure) { throw new ServletException("Controlled concurrent callback failed",failure); }
                    }
                    var marker=(IdpAuthorizationObservationContextBO) captured.get().get(NativeGraphQlSecurityInterceptor.OBSERVATION_MARKER_KEY);
                    assertThat(marker.isConfirmed()).isTrue();
                });
                assertThat(fixture.events.stream().filter(event->"AUTHORIZATION_DENIED".equals(event.get("reasonCode"))).count()).isEqualTo(1);
                Mockito.verifyNoInteractions(repository);
                var marker=(IdpAuthorizationObservationContextBO) captured.get().get(NativeGraphQlSecurityInterceptor.OBSERVATION_MARKER_KEY);
                assertThat(marker.isActive()).isFalse();
                var lateEnvironment=DataFetchingEnvironmentImpl.newDataFetchingEnvironment().graphQLContext(captured.get()).build();
                var late=context.getBean(NativeGraphQlSecurityInstrumentation.class).instrumentDataFetcher(env->{throw new AssertionError("Closed request cannot run business");},null,null);
                Assertions.assertThatThrownBy(()->late.get(lateEnvironment)).isInstanceOf(AuthorizationService.RuntimeUnavailableException.class);
                SecurityContextHolder.clearContext(); top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder.clear(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); MDC.clear();
            });
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void asyncGraphQlDenialAuditFailureIsUnavailableAndNeverOrdinaryForbidden(@TempDir Path temporary) throws Exception {
        var repository = mock(UserRepository.class);
        try (var fixture = new SecurityFixture(temporary)) {
            Mockito.doAnswer(call -> {
                StreamAddParams<String,String> fields=call.getArgument(0);
                if ("DENIED".equals(fields.getEntries().get("result"))) { throw new RedisException("private-broker-address"); }
                fixture.events.add(Map.copyOf(fields.getEntries())); return new StreamMessageId(fixture.events.size(),1);
            }).when(fixture.auditStream).add(ArgumentMatchers.any(StreamAddArgs.class));
            graphRunner(repository,fixture).run(context -> {
                assertThat(context).hasNotFailed();
                var mvc=MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain",Filter.class)).build();
                var result=mvc.perform(MockMvcRequestBuilders.post("/graphql").contentType("application/json")
                                .header("Authorization","Bearer "+fixture.token("1001",Set.of("openid"))).content("{\"query\":\"query AuditFailure { user(id: \\\"1001\\\") { id } }\",\"operationName\":\"AuditFailure\"}"))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.errors[0].extensions.code").value("OAUTH_AUDIT_DELIVERY_UNAVAILABLE"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.errors[0].extensions.classification").value("INTERNAL_ERROR")).andReturn();
                assertThat(result.getResponse().getContentAsString()).doesNotContain("private-broker-address"); Mockito.verifyNoInteractions(repository);
            });
        }
    }

    @Test
    void asyncTimeoutClosesSharedAuthorityBeforeALateCallback(@TempDir Path temporary) throws Exception {
        var repository=mock(UserRepository.class);
        try (var fixture=new SecurityFixture(temporary)) {
            graphRunner(repository,fixture).run(context -> {
                assertThat(context).hasNotFailed();
                var request=new MockHttpServletRequest("POST","/graphql"); request.setAsyncSupported(true);
                var response=new MockHttpServletResponse();
                var captured=new AtomicReference<GraphQLContext>();
                SecurityContextHolder.getContext().setAuthentication(fixture.authenticatedUser("1001",Set.of("egon-cola-source-web:user:read")));
                top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder.set(new top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext("subject-1001",Set.of(),"trace-1"));
                org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request,response));
                fixture.observationFilter.doFilter(request,response,(incoming,outgoing)-> {
                    var web=new WebGraphQlRequest(URI.create("https://organization.example.test/graphql"),new HttpHeaders(),
                            new LinkedMultiValueMap<>(),Map.of(),Map.of("query","{ user(id: \"1001\") { id } }"),"request-1",Locale.ENGLISH);
                    context.getBean(NativeGraphQlSecurityInterceptor.class).intercept(web,next->{captured.set(next.toExecutionInput().getGraphQLContext());return Mono.empty();});
                    request.startAsync(request,response);
                });
                var marker=(IdpAuthorizationObservationContextBO)captured.get().get(NativeGraphQlSecurityInterceptor.OBSERVATION_MARKER_KEY);
                assertThat(marker.isActive()).isTrue();
                var async=(MockAsyncContext)request.getAsyncContext();
                assertThat(async.getListeners()).hasSize(1); async.getListeners().getFirst().onTimeout(new AsyncEvent(async));
                assertThat(marker.isActive()).isFalse();
                var environment=DataFetchingEnvironmentImpl.newDataFetchingEnvironment().graphQLContext(captured.get()).build();
                var callback=context.getBean(NativeGraphQlSecurityInstrumentation.class).instrumentDataFetcher(env->{throw new AssertionError("Timed-out authority cannot enter business");},null,null);
                Assertions.assertThatThrownBy(()->callback.get(environment)).isInstanceOf(AuthorizationService.RuntimeUnavailableException.class);
                Mockito.verifyNoInteractions(repository);
                SecurityContextHolder.clearContext(); top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder.clear(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); MDC.clear();
            });
        }
    }

    /** Shared explicit test composition; production auto-configuration remains disabled in the external-free test profile. */
    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    public static class SecurityTestBeans {
        @Bean(name = "nativeSecurityFixture", destroyMethod = "close")
        SecurityFixture fixture() throws Exception { return new SecurityFixture(Files.createTempDirectory("web-security-test-")); }
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
        public static final String SYSTEM = "egon-cola-source-web";
        public static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");
        public static final URI RESOURCE = URI.create("https://organization.example.test");
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
            settings.setResourceServerId("organization-resource"); settings.setResourceUri(SecurityFixture.RESOURCE);
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
            when(resourceStates.read("organization-resource")).thenReturn(Optional.of(new IdentityResourceServerState(
                    "organization-resource", SecurityFixture.RESOURCE, "sample", SecurityFixture.SYSTEM, "test", ResourceServerStatus.ACTIVE, 7)));
            verifier = IdpJwtVerifier.createConfigured(null, resourceStates,
                    mock(IdentityOAuthClientStateReader.class), "organization-resource", SecurityFixture.RESOURCE, "platform", clock,
                    managers, revocations, Mappers.getMapper(VerifiedPrincipalConverter.class), null, observations);
            identityFilter = IdpBearerAuthenticationFilter.create(
                    new UserAccessTokenVerifier(verifier),
                    new ServiceAccessTokenVerifier(verifier), endpointPolicy, mapper,
                    new VerifiedTenantContextFilter(), verifier);
            observationFilter = new IdpSecurityObservationFilter(observations, endpointPolicy);
            properties.setEnabled(true); properties.setRegisterFilter(false); properties.setSystemCode(SecurityFixture.SYSTEM); properties.getUserScopePolicy().setEnabled(true);
            var policy = new LinkedHashMap<String,Set<String>>();
            for (String intent : List.of("user:create", "user:read", "role:assign", "permission:grant", "permission:read", "grade:create", "grade:read", "school-class:create", "school-class:read", "school-class:assign-user")) {
                String permission = SecurityFixture.SYSTEM+":"+intent; policy.put(permission, Set.of(permission)); permissions.add(permission);
            }
            properties.getUserScopePolicy().setPermissionScopes(policy);
            for (String resource : List.of("api.user.create", "api.user.read", "api.role.assign", "api.permission.grant", "api.permission.read",
                    "api.grade.create", "api.grade.read", "api.school-class.create", "api.school-class.read", "api.school-class.assign-user",
                    "graphql.query.user", "graphql.query.permissionTree", "graphql.query.grade", "graphql.query.schoolClass",
                    "graphql.mutation.createUser", "graphql.mutation.assignRole", "graphql.mutation.grantPermission",
                    "graphql.mutation.createGrade", "graphql.mutation.createSchoolClass", "graphql.mutation.assignUserToSchoolClass")) {
                resources.add(SYSTEM + "." + resource);
            }
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
            var writeSets = Map.of("user:create", Set.of("name", "email"), "role:assign", Set.of("roleCode"),
                    "permission:grant", Set.of("permissionCode"), "grade:create", Set.of("code", "name"),
                    "school-class:create", Set.of("name", "gradeCode"), "school-class:assign-user", Set.of("userId"));
            var mutations = Map.of("user:create", "createUser", "role:assign", "assignRole", "permission:grant", "grantPermission",
                    "grade:create", "createGrade", "school-class:create", "createSchoolClass", "school-class:assign-user", "assignUserToSchoolClass");
            writeSets.forEach((intent, names) -> {
                String permission = SYSTEM + ":" + intent;
                for (String suffix : List.of("api." + intent.replace(':', '.'), "graphql.mutation." + mutations.get(intent))) {
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
            claims.put("iss","https://auth.example.test/tenants/"+tenant); claims.put("sub",service ? "organization-service" : "subject-"+tenant); claims.put("aud",List.of(SecurityFixture.RESOURCE.toASCIIString()));
            claims.put("iat",NOW.minusSeconds(1).getEpochSecond()); claims.put("nbf",NOW.minusSeconds(1).getEpochSecond()); claims.put("exp",NOW.plusSeconds(300).getEpochSecond());
            claims.put("jti",Long.toString(tokenIds.incrementAndGet())); claims.put("tenant_id",tenant); claims.put("client_id",service ? "organization-service" : "browser-client"); claims.put("principal_type",service ? "SERVICE" : "USER");
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
