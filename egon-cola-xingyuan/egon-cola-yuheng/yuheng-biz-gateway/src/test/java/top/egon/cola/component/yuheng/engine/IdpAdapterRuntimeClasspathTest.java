package top.egon.cola.component.yuheng.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class IdpAdapterRuntimeClasspathTest {

    @Test
    void executableEngineCarriesIdentityAndBizAppScopeAdapters() {
        assertDoesNotThrow(() -> Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.gateway.autoconfigure."
                        + "IdpGatewayAdapterAutoConfiguration"));
        assertDoesNotThrow(() -> Class.forName(
                "top.egon.cola.platform.tianquan.jianshen.gateway.autoconfigure."
                        + "Rbac3GatewayAdapterAutoConfiguration"));
    }
}
