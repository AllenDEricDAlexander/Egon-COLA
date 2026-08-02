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

    @Test
    void declaresOnlySixNonSecretRuntimePolicyKeys() {
        Set<String> keys = new HashSet<>();

        for (Field field : IdpDdcValueDeclarations.class.getDeclaredFields()) {
            DdcValue value = field.getAnnotation(DdcValue.class);
            if (value != null) {
                keys.add(value.key());
                assertFalse(value.refreshable());
                assertFalse(value.key().matches(
                        ".*(password-file|private-key|secret|credential).*"
                ));
            }
        }

        assertEquals(AtomicIdpRuntimePolicy.CONFIG_KEYS, keys);
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
