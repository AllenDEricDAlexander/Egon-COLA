package top.egon.cola.archetype.source.web.adapter.graphql;

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
import org.springframework.context.annotation.Role;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.access.AccessDeniedException;
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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.validation.Validation;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.ContextConfiguration;
import top.egon.cola.archetype.source.web.adapter.handler.OrganizationGraphQlExceptionResolver;
import top.egon.cola.archetype.source.web.adapter.user.graphql.UserResolver;
import top.egon.cola.archetype.source.web.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.web.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.manage.impl.UserManageImpl;
import top.egon.cola.archetype.source.web.application.user.pojo.convertor.UserConverter;
import top.egon.cola.archetype.source.web.application.user.validators.UserApplicationValidator;
import top.egon.cola.archetype.source.web.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.web.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.web.domain.user.entities.User;
import top.egon.cola.archetype.source.web.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.web.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@GraphQlTest(UserResolver.class)
@ContextConfiguration(classes = {UserResolver.class, OrganizationGraphQlExceptionResolver.class,
        NativeGraphQlSecurityContractTest.ReadFixtureConfiguration.class})
public class NativeGraphQlSecurityContractTest {
    private static final UserDomainService DATA = mock(UserDomainService.class);
    @Autowired
    private GraphQlTester graphQl;

    @Test
    void namedUserQueryCannotReadThroughTheActualSchemaWithoutVerifiedAuthority() {
        when(DATA.findById(new UserId(1001L))).thenReturn(Optional.of(
                new User(new UserId(1001L), "Reader", "reader@example.test", UserStatus.ACTIVE)));
        graphQl.document("query NativeWebUser($id: ID!) { user(id: $id) { id name email status roleCodes } }")
                .operationName("NativeWebUser").variable("id", "1001").execute()
                .errors().satisfy(errors -> assertThat(errors).as("Missing trusted callback context must reject the actual query").isNotEmpty());
        verifyNoInteractions(DATA);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    public static class ReadFixtureConfiguration {
        @Bean(name = "nativeSecurityFixture", destroyMethod = "close")
        SecurityFixture fixture() throws Exception { return new SecurityFixture(Files.createTempDirectory("web-adapter-security-test-")); }

        @Bean("rbac3FieldWriteAuthorizationComponent")
        Rbac3FieldWriteAuthorizationComponent fields(SecurityFixture security) { return security.fields; }

        @Bean("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
        Rbac3StarterProperties properties(SecurityFixture security) { return security.properties; }

        @Bean("rbac3MethodAuthorizationInterceptor")
        @org.springframework.context.annotation.Role(org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE)
        AuthorizationManagerBeforeMethodInterceptor advisor(SecurityFixture security) { return security.advisor; }

        @Bean(name = "validatorFactory", destroyMethod = "close")
        jakarta.validation.ValidatorFactory validators() { return Validation.buildDefaultValidatorFactory(); }

        @Bean("userManage")
        UserManage users(jakarta.validation.ValidatorFactory validators) {
            return new UserManageImpl(DATA, new UserApplicationValidator(new ValidationUtils(validators.getValidator())),
                    Mappers.getMapper(UserConverter.class), mock(CommandIdempotencyService.class), mock(OrganizationEventService.class));
        }

        @Bean("roleManage")
        RoleManage roles() { return mock(RoleManage.class); }

        @Bean("permissionManage")
        PermissionManage permissions() { return mock(PermissionManage.class); }
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
            // SERVICE verification reads this store; the shared USER-only wire fixtures do not.
            Mockito.lenient().when(resourceStates.read("organization-resource")).thenReturn(Optional.of(new IdentityResourceServerState(
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

        public top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcBearerServerInterceptor rpcInterceptor() {
            identityProperties.getRpc().setEnabled(true);
            identityProperties.getRpc().getProvider().setEnabled(true);
            var scopes = Map.of("UserService/CreateUser", "user:create", "UserService/GetUser", "user:read", "RoleService/AssignRole", "role:assign",
                    "PermissionService/GrantPermission", "permission:grant", "PermissionService/GetPermissionTree", "permission:read",
                    "GradeService/CreateGrade", "grade:create", "GradeService/GetGrade", "grade:read", "SchoolClassService/CreateSchoolClass", "school-class:create",
                    "SchoolClassService/GetSchoolClass", "school-class:read", "SchoolClassService/AssignUser", "school-class:assign-user");
            scopes.forEach((method, intent) -> identityProperties.getRpc().getProvider().getMethods().put("top.egon.cola.organization.facade.rpc.v1." + method,
                    IdpStarterProperties.RpcProviderMethodProperties.builder().required(true)
                            .principalKind(IdpStarterProperties.RpcPrincipalKindEnum.SERVICE)
                            .scopeContext(top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT)
                            .expectedResourceUri(RESOURCE).requiredScopes(Set.of(SYSTEM + ":" + intent)).build()));
            return top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcBearerServerInterceptor.createConfigured(
                    new UserAccessTokenVerifier(verifier), verifier, identityProperties, observations, validator);
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
