package top.egon.cola.component.gateway.test.live;

import org.junit.jupiter.api.Test;

class GatewayLiveTopologyContractTest {

    private final GatewayLiveTopologyIT topology = new GatewayLiveTopologyIT();

    @Test
    void everyDdcClientUsesInfrastructureRedisCoordinates() {
        topology.everyDdcClientUsesInfrastructureRedisCoordinates();
    }

    @Test
    void httpProviderSpecUsesSingleServiceVersionSource() {
        topology.httpProviderSpecUsesSingleServiceVersionSource();
    }
}
