package top.egon.cola.platform.tianquan.shoubing.admin.support.tianshu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.tianshu.annotation.DdcValue;
import top.egon.cola.component.tianshu.api.client.DdcConfigClient;
import top.egon.cola.component.tianshu.service.refresh.DefaultDdcConfigApplierRegistry;
import top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcAutoConfiguration;
import top.egon.cola.component.rpc.tianshu.client.config.RpcDdcConfigClient;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DdcPolicyConfigTest {

    private static final Set<String> EXPECTED_EXPRESSIONS = Set.of(
            "${tianquan-shoubing.token.access-ttl:900}",
            "${tianquan-shoubing.token.refresh-ttl:604800}",
            "${tianquan-shoubing.authorization-code.ttl:60}",
            "${tianquan-shoubing.login.max-failures:5}",
            "${tianquan-shoubing.login.lock-duration:900}",
            "${tianquan-shoubing.password.max-concurrency:8}"
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
                    throw new AssertionError("Tianquan-Shoubing keys need exact appliers");
                });
        AtomicIdpRuntimePolicy policy = new AtomicIdpRuntimePolicy();
        InitializingBean registrar = new DdcPolicyConfig()
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

    @Test
    void enabledDdcUsesTheDirectRpcConfigPort() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DdcRpcAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.application.name=tianquan-shoubing-admin-test",
                        "egon.cola.component.tianshu.enabled=true",
                        "egon.cola.component.tianshu.biz-code=identity",
                        "egon.cola.component.tianshu.env=test",
                        "egon.cola.component.tianshu.app-code=tianquan-shoubing-admin",
                        "egon.cola.component.tianshu.rpc.target=dns:///127.0.0.1:19080",
                        "egon.cola.component.tianshu.rpc.tls.development-plaintext=true",
                        "egon.cola.component.tianshu.rpc.auth.runtime.access-key=test",
                        "egon.cola.component.tianshu.rpc.auth.runtime.secret-key=test"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcConfigClient.class);
                    assertThat(context.getBean(DdcConfigClient.class))
                            .isInstanceOf(RpcDdcConfigClient.class);
                });
    }
}
