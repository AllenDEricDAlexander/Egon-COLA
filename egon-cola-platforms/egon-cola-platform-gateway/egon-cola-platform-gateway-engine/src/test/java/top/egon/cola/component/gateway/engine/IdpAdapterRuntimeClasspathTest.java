package top.egon.cola.component.gateway.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class IdpAdapterRuntimeClasspathTest {

    @Test
    void executableEngineCarriesIdentityAndBizAppScopeAdapters() {
        assertDoesNotThrow(() -> Class.forName(
                "top.egon.cola.platform.idp.gateway.autoconfigure."
                        + "IdpGatewayAdapterAutoConfiguration"));
        assertDoesNotThrow(() -> Class.forName(
                "top.egon.cola.platform.rbac3.gateway.autoconfigure."
                        + "Rbac3GatewayAdapterAutoConfiguration"));
    }
}
