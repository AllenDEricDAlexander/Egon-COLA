package top.egon.cola.component.gateway.test.live;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class GatewayHttpProvidersLiveIT {

    @Test
    @EnabledIfSystemProperty(named = "gateway.live.test", matches = "true")
    void verifiesMvcAndWebFluxProviderLifecycle() throws Exception {
        new GatewayLiveTopologyIT().verifyHttpProvidersLifecycle();
    }
}
