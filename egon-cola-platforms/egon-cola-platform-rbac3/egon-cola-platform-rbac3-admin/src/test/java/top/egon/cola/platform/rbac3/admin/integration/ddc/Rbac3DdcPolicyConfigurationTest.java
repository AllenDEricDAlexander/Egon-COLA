package top.egon.cola.platform.rbac3.admin.integration.ddc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ddc.annotation.DdcValue;
import top.egon.cola.component.ddc.service.DdcConfigApplier;
import top.egon.cola.component.ddc.service.DefaultDdcConfigApplierRegistry;
import top.egon.cola.platform.rbac3.admin.config.Rbac3AdminProperties;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class Rbac3DdcPolicyConfigurationTest {

    private static final Map<String, Declaration> EXPECTED_DECLARATIONS = declarations();

    @Test
    void declaresFiveNonReflectiveRefreshablePolicyInputs() {
        Map<String, Declaration> actual = new LinkedHashMap<>();

        for (Field field : Rbac3DdcValueDeclarations.class.getDeclaredFields()) {
            DdcValue annotation = field.getAnnotation(DdcValue.class);
            if (annotation != null) {
                actual.put(annotation.key(), new Declaration(
                        annotation.value(),
                        annotation.defaultValue(),
                        annotation.type(),
                        annotation.required(),
                        annotation.refreshable()));
            }
        }

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(EXPECTED_DECLARATIONS);
    }

    @Test
    void registersExactAppliersBeforeFreezeWithDeterministicPriorities() throws Exception {
        AtomicBoolean fallbackUsed = new AtomicBoolean();
        DefaultDdcConfigApplierRegistry registry = new DefaultDdcConfigApplierRegistry(
                (key, value, version) -> fallbackUsed.set(true));
        AtomicRbac3RuntimePolicy policy = policyWithDefaults();
        InitializingBean registrar = new Rbac3DdcPolicyConfiguration()
                .rbac3DdcPolicyRegistrar(registry, policy);

        registrar.afterPropertiesSet();

        assertPriority(registry, AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 0);
        assertPriority(registry, AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, 0);
        assertPriority(registry, AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, 10);
        assertPriority(registry, AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, 20);
        assertPriority(registry, AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, 30);
        assertThat(fallbackUsed).isFalse();

        assertThatIllegalArgumentException()
                .isThrownBy(registrar::afterPropertiesSet)
                .withMessageContaining("already registered");

        registry.freeze();
        assertThatIllegalStateException()
                .isThrownBy(() -> registry.registerExact(
                        "rbac3.another", (key, value, version) -> {
                        }))
                .withMessageContaining("frozen");
    }

    @Test
    void forwardsTheExactKeyValueAndVersionAndSupportsOrderedSnapshotApplication()
            throws Exception {
        AtomicRbac3RuntimePolicy policy = policyWithDefaults();
        DefaultDdcConfigApplierRegistry registry = new DefaultDdcConfigApplierRegistry(
                (key, value, version) -> {
                    throw new AssertionError("RBAC3 keys must not use the reflective fallback");
                });
        new Rbac3DdcPolicyConfiguration()
                .rbac3DdcPolicyRegistrar(registry, policy)
                .afterPropertiesSet();
        registry.freeze();

        apply(registry, AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, "172800", 11L);
        apply(registry, AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, "86400", 12L);
        apply(registry, AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, "28800", 13L);

        assertThat(policy.current().refreshTokenTtl()).isEqualTo(Duration.ofDays(2));
        assertThat(policy.current().sessionAbsoluteTimeout()).isEqualTo(Duration.ofDays(1));
        assertThat(policy.current().sessionIdleTimeout()).isEqualTo(Duration.ofHours(8));
        assertThat(policy.current().configVersions()).contains(
                Map.entry(AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, 11L),
                Map.entry(AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, 12L),
                Map.entry(AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, 13L));

        String secretLikeRawValue = "not-a-number-do-not-log";
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> apply(
                        registry,
                        AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                        secretLikeRawValue,
                        14L))
                .hasMessageNotContaining(secretLikeRawValue);
    }

    @Test
    void createsDeclarationsAndRegistrarOnlyWhenDdcIsEnabled() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(Rbac3DdcPolicyConfiguration.class)
                .withBean(AtomicRbac3RuntimePolicy.class, this::policyWithDefaults)
                .withBean(DefaultDdcConfigApplierRegistry.class,
                        () -> new DefaultDdcConfigApplierRegistry((key, value, version) -> {
                        }));

        runner.run(context -> {
            assertThat(context).doesNotHaveBean(Rbac3DdcValueDeclarations.class);
            assertThat(context).doesNotHaveBean("rbac3DdcPolicyRegistrar");
        });

        runner.withPropertyValues("egon.cola.component.ddc.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Rbac3DdcValueDeclarations.class);
                    assertThat(context).hasBean("rbac3DdcPolicyRegistrar");
                    assertThat(context.getBean(DefaultDdcConfigApplierRegistry.class)
                            .resolve(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY))
                            .isInstanceOf(Rbac3DdcPolicyApplier.class);
                });
    }

    private void assertPriority(
            DefaultDdcConfigApplierRegistry registry,
            String key,
            int priority) {
        assertThat(registry.resolve(key))
                .isInstanceOf(Rbac3DdcPolicyApplier.class)
                .extracting(DdcConfigApplier::priority)
                .isEqualTo(priority);
    }

    private void apply(
            DefaultDdcConfigApplierRegistry registry,
            String key,
            String value,
            long version) {
        registry.resolve(key).apply(key, value, version);
    }

    private AtomicRbac3RuntimePolicy policyWithDefaults() {
        return new AtomicRbac3RuntimePolicy(new Rbac3AdminProperties());
    }

    private static Map<String, Declaration> declarations() {
        Map<String, Declaration> expected = new LinkedHashMap<>();
        expected.put(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                new Declaration("rbac3.access-token-ttl-seconds:900", "900", Long.class,
                        true, false));
        expected.put(AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY,
                new Declaration("rbac3.refresh-token-ttl-seconds:604800", "604800", Long.class,
                        true, false));
        expected.put(AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY,
                new Declaration("rbac3.session-idle-timeout-seconds:1800", "1800", Long.class,
                        true, false));
        expected.put(AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY,
                new Declaration("rbac3.session-absolute-timeout-seconds:43200", "43200", Long.class,
                        true, false));
        expected.put(AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY,
                new Declaration("rbac3.maximum-active-roots:16", "16", Integer.class,
                        true, false));
        return expected;
    }

    private record Declaration(
            String value,
            String defaultValue,
            Class<?> type,
            boolean required,
            boolean refreshable) {
    }
}
