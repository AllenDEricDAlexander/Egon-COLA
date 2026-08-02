package top.egon.cola.component.gateway.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdpAdapterRuntimeClasspathTest {

    @Test
    void executableEngineCarriesOnlyTheIdpIdentityAdapter() {
        assertDoesNotThrow(() -> Class.forName(
                "top.egon.cola.platform.idp.gateway.autoconfigure."
                        + "IdpGatewayAdapterAutoConfiguration"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "top.egon.cola.platform.rbac3.gateway.autoconfigure."
                        + "Rbac3GatewayAdapterAutoConfiguration"));
    }
}
