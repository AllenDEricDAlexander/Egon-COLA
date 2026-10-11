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
