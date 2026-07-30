package top.egon.cola.component.gateway.test.live;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class GatewayRuleLifecycleLiveIT {

    @Test
    @EnabledIfSystemProperty(named = "gateway.live.test", matches = "true")
    void verifiesDualEngineReleaseRollbackAndDistributedRateLimit()
            throws Exception {
        new GatewayLiveTopologyIT().verifyHttpProvidersLifecycle();
    }
}
