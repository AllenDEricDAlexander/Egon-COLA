package top.egon.cola.platform.idp.admin.integration.ddc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import top.egon.cola.component.ddc.annotation.DdcValue;
import top.egon.cola.component.ddc.service.DefaultDdcConfigApplierRegistry;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class IdpDdcPolicyConfigurationTest {

    private static final Set<String> EXPECTED_EXPRESSIONS = Set.of(
            "${idp.token.access-ttl:900}",
            "${idp.token.refresh-ttl:604800}",
            "${idp.authorization-code.ttl:60}",
            "${idp.login.max-failures:5}",
            "${idp.login.lock-duration:900}",
            "${idp.password.max-concurrency:8}"
    );

    @Test
    void declaresOnlySixNonSecretRuntimePolicyKeys() {
        Set<String> expressions = new HashSet<>();

        for (Field field : IdpDdcValueDeclarations.class.getDeclaredFields()) {
            DdcValue value = field.getAnnotation(DdcValue.class);
            if (value != null) {
                expressions.add(value.value());
                assertFalse(value.refreshable());
                assertFalse(value.value().matches(
                        ".*(password-file|private-key|secret|credential).*"
                ));
            }
        }

        assertEquals(EXPECTED_EXPRESSIONS, expressions);
    }

    @Test
    void registersExactAppliersBeforeRegistryFreeze() throws Exception {
        DefaultDdcConfigApplierRegistry registry =
                new DefaultDdcConfigApplierRegistry((key, value, version) -> {
                    throw new AssertionError("IdP keys need exact appliers");
                });
        AtomicIdpRuntimePolicy policy = new AtomicIdpRuntimePolicy();
        InitializingBean registrar = new IdpDdcPolicyConfiguration()
                .idpDdcPolicyRegistrar(registry, policy);

        registrar.afterPropertiesSet();
        registry.freeze();

        for (String key : AtomicIdpRuntimePolicy.CONFIG_KEYS) {
            assertInstanceOf(
                    IdpDdcPolicyApplier.class,
                    registry.resolve(key)
            );
        }
    }
}
