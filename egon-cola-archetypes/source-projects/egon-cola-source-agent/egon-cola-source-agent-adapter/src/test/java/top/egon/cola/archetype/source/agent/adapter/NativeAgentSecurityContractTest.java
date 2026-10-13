package top.egon.cola.archetype.source.agent.adapter;

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

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import top.egon.cola.archetype.source.agent.adapter.knowledge.controller.KnowledgeBaseController;
import top.egon.cola.archetype.source.agent.adapter.knowledge.controller.KnowledgeDocumentController;
import top.egon.cola.archetype.source.agent.adapter.knowledge.controller.KnowledgeQaController;
import top.egon.cola.archetype.source.agent.adapter.research.controller.DeepResearchController;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeBaseManage;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeDocumentManage;
import top.egon.cola.archetype.source.agent.application.knowledge.manage.KnowledgeQaManage;
import top.egon.cola.archetype.source.agent.application.knowledge.config.KnowledgeRuntimeProperties;
import top.egon.cola.archetype.source.agent.application.research.manage.DeepResearchManage;
import top.egon.cola.archetype.source.agent.application.research.config.DeepResearchRuntimeProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Actual Spring method advisors and SDK boundaries; original wire callers reuse the same field guard. */
public class NativeAgentSecurityContractTest {
    @Test
    void allFourteenActualMethodsRequireBothScopesAndPdp(@TempDir Path temporary) throws Exception {
        try (var fixture = new SecurityFixture(temporary); var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SecurityFixture.class, () -> fixture);
            context.register(MethodBeans.class);
            context.refresh();
            SecurityContextHolder.getContext().setAuthentication(fixture.authenticatedUser("1001", Set.of("openid")));
            rejectAll(context, 14);
            assertThat(fixture.permissionChecks).isZero();
            Set<String> scopes = Set.copyOf(fixture.permissions);
            fixture.permissions.clear();
            SecurityContextHolder.getContext().setAuthentication(fixture.authenticatedUser("1001", scopes));
            rejectAll(context, 14);
            assertThat(fixture.permissionChecks).isGreaterThan(0);
            Mockito.verifyNoInteractions(context.getBean("knowledgeBaseManage"), context.getBean("knowledgeDocumentManage"),
                    context.getBean("knowledgeQaManage"), context.getBean("deepResearchManage"));
        }
    }

    private static void rejectAll(AnnotationConfigApplicationContext context, int expected) {
        int inspected = 0;
        for (Class<?> type : List.of(KnowledgeBaseController.class, KnowledgeDocumentController.class,
                KnowledgeQaController.class, DeepResearchController.class)) {
            Object proxy = context.getBean(type);
            for (var method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(org.springframework.security.access.prepost.PreAuthorize.class)) { continue; }
                Object[] arguments = new Object[method.getParameterCount()];
                for (int i = 0; i < arguments.length; i++) {
                    if (method.getParameterTypes()[i] == String.class) arguments[i] = "1001";
                    else if (method.getParameterTypes()[i] == int.class) arguments[i] = 1;
                }
                Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() -> method.invoke(proxy, arguments));
                assertThat(failure).as(method.getName()).isInstanceOf(InvocationTargetException.class);
                assertThat(failure.getCause()).isInstanceOfAny(org.springframework.security.access.AccessDeniedException.class,
                        DefaultAuthorizationService.AuthorizationDeniedException.class);
                inspected++;
            }
        }
        assertThat(inspected).isEqualTo(expected);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodBeans {
        @Bean("rbac3MethodAuthorizationInterceptor") @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        AuthorizationManagerBeforeMethodInterceptor advisor(SecurityFixture fixture) { return fixture.advisor; }
        @Bean("knowledgeBaseManage") KnowledgeBaseManage bases() { return mock(KnowledgeBaseManage.class); }
        @Bean("knowledgeDocumentManage") KnowledgeDocumentManage documents() { return mock(KnowledgeDocumentManage.class); }
        @Bean("knowledgeQaManage") KnowledgeQaManage qa() { return mock(KnowledgeQaManage.class); }
        @Bean("deepResearchManage") DeepResearchManage research() { return mock(DeepResearchManage.class); }
        @Bean("knowledgeBaseController") KnowledgeBaseController basesController(SecurityFixture f, KnowledgeBaseManage m) { return new KnowledgeBaseController(f.fields, f.properties, m); }
        @Bean("knowledgeDocumentController") KnowledgeDocumentController documentsController(SecurityFixture f, KnowledgeDocumentManage m) { return new KnowledgeDocumentController(f.fields, f.properties, m); }
        @Bean("knowledgeQaController") KnowledgeQaController qaController(KnowledgeQaManage m) { return new KnowledgeQaController(m, new KnowledgeRuntimeProperties(null, null)); }
        @Bean("deepResearchController") DeepResearchController researchController(DeepResearchManage m, SecurityFixture f) { return new DeepResearchController(m, new DeepResearchRuntimeProperties(4, Duration.ofMinutes(2), 8, Duration.ofSeconds(15)), f.clock); }
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

        /** Uses the real Spring authentication guard for wire fixtures that do not create a host application context. */
        public jakarta.servlet.Filter[] httpBoundary() {
            var translation = new org.springframework.security.web.access.ExceptionTranslationFilter((request, response, failure) -> {
                boolean delivered = IdpSecurityObservationFilter.beforeAuthenticationFailure(request);
                response.setStatus(delivered ? 401 : 503);
                response.setHeader("Cache-Control", "no-store");
                if (delivered) { response.setHeader("WWW-Authenticate", "Bearer"); }
                response.setContentType("application/json");
                mapper.writeValue(response.getOutputStream(), Map.of("code", delivered ? "AUTHENTICATION_REQUIRED" : "OAUTH_AUDIT_DELIVERY_UNAVAILABLE", "message", "authentication unavailable"));
            });
            var required = new org.springframework.security.web.access.intercept.AuthorizationFilter(
                    org.springframework.security.authorization.AuthenticatedAuthorizationManager.authenticated());
            required.setFilterAsyncDispatch(false);
            return new jakarta.servlet.Filter[]{observationFilter, identityFilter, rbacFilter, translation, required};
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
