package top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityMetricsComponent;

class IdpSecurityObservationAutoConfigurationTest {
    private static final String PREFIX = "egon.cola.platform.tianquan.shoubing.";
    @TempDir Path temporary;
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final RedissonClient primaryRedis = mock(RedissonClient.class);

    @BeforeEach void prepare() throws Exception {
        validator.afterPropertiesSet();
        var key = temporary.resolve("audit.key");
        Files.writeString(key, "0123456789abcdef0123456789abcdef");
        Files.setPosixFilePermissions(key, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
    }
    @AfterEach void close() { validator.close(); registry.close(); }

    @Test
    @SuppressWarnings("unchecked")
    void uniqueRegistryAndPrimaryRedisAliasAreReusedWithoutInitializationCycles() {
        var secondary = new SimpleMeterRegistry();
        runner().withBean("primaryRegistry", MeterRegistry.class, () -> registry, definition -> definition.setPrimary(true))
                .withBean("secondaryRegistry", MeterRegistry.class, () -> secondary)
                .withBean("unrelatedRedis", RedissonClient.class, () -> mock(RedissonClient.class))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(TokenUsageAuditComponent.class).hasSingleBean(IdpSecurityMetricsRecorder.class);
                    assertThat(context.getBean("idpSecurityMetricsRecorder")).isInstanceOf(IdpSecurityMetricsComponent.class);
                    assertThat(context.getBeansOfType(MeterRegistry.class)).hasSize(2);
                    assertThat(((Supplier<MeterRegistry>) context.getBean("idpMeterRegistrySupplier")).get()).isSameAs(registry);
                    assertThat(context.getBean("idpAuditRedissonClient")).isSameAs(primaryRedis);
                    assertThat(context.getBean("idpAuditSpoolLease")).isSameAs(context.getBean("tianquanAuditSpoolLease"));
                    assertThat(context.getBean("idpSecurityAuditReady")).isSameAs(context.getBean("tianquanSecurityAuditReady"));
                    assertThat(context.getBean("idpSecurityAuditReady", BooleanSupplier.class).getAsBoolean()).isTrue();
                });
        verify(primaryRedis, never()).shutdown();
        secondary.close();
    }

    @Test
    void enabledObservationRequiresAUniqueRegistryAndADeclaredReceiver() {
        runner().run(context -> assertThat(context).hasFailed());
        runner().withBean("first", MeterRegistry.class, () -> registry)
                .withBean("second", MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> assertThat(context).hasFailed());
        runner().withBean("registry", MeterRegistry.class, () -> registry)
                .withPropertyValues(PREFIX + "usage-observation.receiver=")
                .run(context -> assertThat(context).hasFailed());
        runner().withBean("registry", MeterRegistry.class, () -> registry)
                .withPropertyValues(PREFIX + "usage-observation.receiver=TENANT_1001")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void exclusiveLeaseAndDynamicReadinessRetainUnconfirmedAndDeadEvents() throws Exception {
        var enabled = runner().withBean("registry", MeterRegistry.class, () -> registry);
        FileChannel[] held = new FileChannel[1];
        enabled.run(context -> {
            assertThat(context).hasNotFailed();
            held[0] = context.getBean("idpAuditSpoolLease", FileChannel.class);
            var ready = context.getBean("idpSecurityAuditReady", BooleanSupplier.class);
            enabled.run(other -> assertThat(other).hasFailed());
            try {
                var pending = temporary.resolve("spool/pending.evt");
                Files.writeString(pending, "unconfirmed");
                assertThat(ready.getAsBoolean()).isFalse();
                Files.writeString(pending.resolveSibling("pending.evt.sent"), "confirmed");
                assertThat(ready.getAsBoolean()).isTrue();
                Files.writeString(pending.resolveSibling("pending.evt.dead"), "retained");
                assertThat(ready.getAsBoolean()).isFalse();
                Files.delete(pending.resolveSibling("pending.evt.dead"));
                Files.delete(pending.resolveSibling("pending.evt.sent"));
                Files.delete(pending);
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        });
        assertThat(held[0].isOpen()).isFalse();
        enabled.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void configuredObserverRejectsDeadPendingAndFullSpoolsAndRecoversWithTheSameReadinessOwner() {
        var producer = mock(TokenUsageAuditComponent.class);
        org.mockito.Mockito.when(producer.appendStrict(org.mockito.ArgumentMatchers.any())).thenReturn(new org.redisson.api.StreamMessageId(1, 1));
        var issuer = java.net.URI.create("https://idp.example/tenants/1001"); var resource = java.net.URI.create("https://api.example/orders");
        var now = java.time.Instant.now();
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("controlled-completed-verification-fixture").header("alg", "RS256")
                .issuer(issuer.toASCIIString()).subject("controlled-subject").claim("client_id", "controlled-client").issuedAt(now).expiresAt(now.plusSeconds(300)).build();
        var trusted = new top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.TrustedIssuerVO(issuer,
                java.net.URI.create(issuer + "/.well-known/jwks.json"), "tenant:1001", top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.RealmType.TENANT,
                "1001", top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry.Status.ACTIVE, 1);
        var verified = top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO.builder().jwt(jwt).trustedIssuer(trusted)
                .subject("controlled-subject").tenantId("1001").tokenId("17").keyId("key-1").authorizationId("19").clientRegistrationId("23")
                .principalType(top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType.USER).expiresAt(now.plusSeconds(300))
                .securityVersion(new top.egon.cola.platform.tianquan.shoubing.contract.OAuthSecurityVersionBO(0, 0, 0L)).build();
        verified.validate(); org.mockito.Mockito.when(producer.reference(issuer, "17")).thenReturn("c".repeat(64));
        var successful = new top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Valid<>(
                new top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal("controlled-subject", "1001", "17", Set.of(resource.toASCIIString()), now, now.plusSeconds(300),
                        top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.of("PASSWORD", now)), Set.of("orders:read"));
        runner().withBean("registry", MeterRegistry.class, () -> registry)
                .withBean("tokenUsageAuditComponent", TokenUsageAuditComponent.class, () -> producer)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var observations = context.getBean("idpObservedVerificationComponent", top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class);
                    var ready = context.getBean("idpSecurityAuditReady", BooleanSupplier.class);
                    for (String fault : java.util.List.of("dead", "pending", "capacity")) {
                        assertThat(ready.getAsBoolean()).isTrue(); assertThat(observations.authenticationFailure()).isTrue();
                        assertThat(observations.observeCompleted(successful, verified, resource, "controlled"))
                                .isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Valid.class);
                        org.mockito.Mockito.clearInvocations(producer);
                        Path file = temporary.resolve("spool/controlled." + (fault.equals("pending") ? "evt" : fault));
                        try {
                            Files.writeString(file, "controlled readiness fault");
                            if (fault.equals("capacity")) {
                                try (var content = FileChannel.open(file, java.nio.file.StandardOpenOption.WRITE)) {
                                    content.position(1_048_575); content.write(java.nio.ByteBuffer.wrap(new byte[]{0})); content.force(true);
                                }
                            }
                            assertThat(ready.getAsBoolean()).isFalse();
                            assertThat(observations.authenticationFailure()).isFalse();
                            var proof = new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO(java.net.URI.create("https://idp.example/tenants/1001"),
                                    java.net.URI.create("https://api.example/orders"), "a".repeat(64), "b".repeat(64), "c".repeat(64));
                            assertThat(observations.authorizationDenied(proof, "controlled")).isFalse();
                            assertThat(observations.observeCompleted(new top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Invalid<>("invalid"), null,
                                    proof.resource(), "controlled")).isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Unavailable.class);
                            org.mockito.Mockito.verifyNoInteractions(producer);
                            assertThat(observations.observeCompleted(successful, verified, resource, "controlled"))
                                    .isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Unavailable.class);
                            verify(producer, never()).appendStrict(org.mockito.ArgumentMatchers.any());
                        } finally { Files.deleteIfExists(file); }
                        assertThat(ready.getAsBoolean()).isTrue(); assertThat(observations.authenticationFailure()).isTrue();
                    }
                    org.mockito.Mockito.clearInvocations(producer);
                    var unavailable = new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent(producer,
                            context.getBean("tokenUsageAuditEventConverterImpl", top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter.class),
                            context.getBean("oauthUsageObservationProperties", top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties.class),
                            context.getBean("idpSecurityMetricsRecorder", IdpSecurityMetricsRecorder.class), context.getBean("idpClock", java.time.Clock.class), validator,
                            java.net.URI.create("https://api.example/orders"), () -> { throw new IllegalStateException("controlled readiness unavailable"); });
                    assertThat(unavailable.authenticationFailure()).isFalse(); org.mockito.Mockito.verifyNoInteractions(producer);
                    context.getBean("idpAuditSpoolLease", FileChannel.class).close();
                    assertThat(ready.getAsBoolean()).isFalse(); assertThat(observations.authenticationFailure()).isFalse();
                    org.mockito.Mockito.verifyNoInteractions(producer);
                });
    }

    @Test
    void readinessConstructorRetainsThePublicAdapterAndQualifiedAutowiredMetadata() throws Exception {
        var type = top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class;
        Class<?>[] legacy = {TokenUsageAuditComponent.class,
                top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter.class,
                top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties.class,
                IdpSecurityMetricsRecorder.class, java.time.Clock.class, jakarta.validation.Validator.class, java.net.URI.class};
        var full = java.util.Arrays.copyOf(legacy, 8); full[7] = BooleanSupplier.class;
        var adapter = type.getConstructor(legacy); var injected = type.getConstructor(full);
        assertThat(adapter.isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class)).isFalse();
        assertThat(injected.isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class)).isTrue();
        var names = java.util.List.of("tokenUsageAuditComponent", "tokenUsageAuditEventConverterImpl", "oauthUsageObservationProperties",
                "idpSecurityMetricsRecorder", "idpClock", "defaultValidator");
        for (int index = 0; index < names.size(); index++) {
            assertThat(adapter.getParameters()[index].getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value()).isEqualTo(names.get(index));
            assertThat(injected.getParameters()[index].getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value()).isEqualTo(names.get(index));
        }
        assertThat(injected.getParameters()[7].getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value()).isEqualTo("idpSecurityAuditReady");
        assertThat(java.lang.reflect.Modifier.isFinal(type.getDeclaredField("auditReady").getModifiers())).isTrue();
    }

    @Test
    void missingOwnerKeyOrRetainedBacklogPreventsReadyStartup() throws Exception {
        var enabled = runner().withBean("registry", MeterRegistry.class, () -> registry);
        Files.delete(temporary.resolve("audit.key"));
        enabled.run(context -> assertThat(context).hasFailed());
        Files.writeString(temporary.resolve("audit.key"), "0123456789abcdef0123456789abcdef");
        Files.setPosixFilePermissions(temporary.resolve("audit.key"), Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        Files.createDirectories(temporary.resolve("spool"));
        Files.writeString(temporary.resolve("spool/orphan.evt"), "retained");
        enabled.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void disabledObservationWorksWithoutTheOptionalMicrometerApi() {
        new ApplicationContextRunner().withClassLoader(new FilteredClassLoader("io.micrometer.core"))
                .withConfiguration(AutoConfigurations.of(IdpSecurityObservationAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("idpSecurityMetricsRecorder")
                        .doesNotHaveBean("idpAuditSpoolLease"));
    }

    @Test
    void enabledObservationCannotSilentlyFallBackWhenTheOptionalMicrometerApiIsAbsent() {
        runner().withClassLoader(new FilteredClassLoader("io.micrometer.core"))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void servletObservationFilterIsNotInstantiatedInAReactiveHost() {
        new ReactiveWebApplicationContextRunner().withConfiguration(configurations())
                .withPropertyValues(properties())
                .withBean("jacksonObjectMapper", ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules())
                .withBean("defaultValidator", jakarta.validation.Validator.class, () -> validator)
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> primaryRedis)
                .withBean("registry", MeterRegistry.class, () -> registry)
                .run(context -> assertThat(context).hasNotFailed().hasBean("idpObservedVerificationComponent")
                        .doesNotHaveBean("idpSecurityObservationFilter").doesNotHaveBean("idpSecurityObservationFilterRegistration"));
        new WebApplicationContextRunner().withConfiguration(configurations())
                .withPropertyValues(properties())
                .withBean("jacksonObjectMapper", ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules())
                .withBean("defaultValidator", jakarta.validation.Validator.class, () -> validator)
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> primaryRedis)
                .withBean("registry", MeterRegistry.class, () -> registry)
                .run(context -> assertThat(context).hasNotFailed().hasBean("idpSecurityObservationFilter")
                        .hasBean("idpSecurityObservationFilterRegistration"));
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(configurations()).withPropertyValues(properties())
                .withBean("jacksonObjectMapper", ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules())
                .withBean("defaultValidator", jakarta.validation.Validator.class, () -> validator)
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> primaryRedis);
    }

    private AutoConfigurations configurations() {
        return AutoConfigurations.of(IdpSecurityObservationAutoConfiguration.class, IdpStarterAutoConfiguration.class);
    }

    private String[] properties() {
        return new String[] { PREFIX + "enabled=true", PREFIX + "issuer=https://idp.example/tenants/1001",
                PREFIX + "jwk-set-uri=https://idp.example/tenants/1001/.well-known/jwks.json",
                PREFIX + "resource-server-id=orders-resource", PREFIX + "resource-uri=https://api.example/orders",
                PREFIX + "usage-observation.enabled=true", PREFIX + "usage-observation.receiver=TIANSHU",
                PREFIX + "usage-observation.fallback-issuer=https://idp.example/tenants/1001",
                PREFIX + "audit.spool-directory=" + temporary.resolve("spool"),
                PREFIX + "audit.hmac-key-file=" + temporary.resolve("audit.key"), PREFIX + "audit.spool-max-bytes=1048576" };
    }
}
