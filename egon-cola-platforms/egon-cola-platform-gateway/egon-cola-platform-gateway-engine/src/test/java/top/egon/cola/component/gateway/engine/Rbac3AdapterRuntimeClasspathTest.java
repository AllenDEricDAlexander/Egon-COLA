package top.egon.cola.component.gateway.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class Rbac3AdapterRuntimeClasspathTest {

    @Test
    void executableEngineCarriesTheRbac3AdapterAutoConfiguration() {
        assertDoesNotThrow(() -> Class.forName(
                "top.egon.cola.platform.rbac3.gateway.autoconfigure."
                        + "Rbac3GatewayAdapterAutoConfiguration"));
    }
}
