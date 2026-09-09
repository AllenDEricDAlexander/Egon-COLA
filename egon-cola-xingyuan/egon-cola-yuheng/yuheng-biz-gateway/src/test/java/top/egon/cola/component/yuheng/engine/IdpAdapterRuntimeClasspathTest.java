package top.egon.cola.component.yuheng.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class IdpAdapterRuntimeClasspathTest {

    @Test
    void executableEngineCarriesIdentityAndBizAppScopeAdapters() {
        assertDoesNotThrow(() -> Class.forName(
                "top.egon.cola.platform.tianquan.shoubing.yuheng.autoconfigure."
                        + "IdpGatewayAdapterAutoConfiguration"));
        assertDoesNotThrow(() -> Class.forName(
                "top.egon.cola.platform.tianquan.jianshen.yuheng.autoconfigure."
                        + "Rbac3GatewayAdapterAutoConfiguration"));
    }
}
