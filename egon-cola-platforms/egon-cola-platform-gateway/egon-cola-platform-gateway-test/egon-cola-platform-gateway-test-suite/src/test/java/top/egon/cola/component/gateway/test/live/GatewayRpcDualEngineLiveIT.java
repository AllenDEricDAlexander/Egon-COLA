package top.egon.cola.component.gateway.test.live;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class GatewayRpcDualEngineLiveIT {

    @Test
    @EnabledIfSystemProperty(named = "gateway.live.test", matches = "true")
    void verifiesRpcConsumerAndHttpRoutingAcrossTwoEngines()
            throws Exception {
        new GatewayLiveTopologyIT().verifyRpcDualEngineLifecycle();
    }
}
