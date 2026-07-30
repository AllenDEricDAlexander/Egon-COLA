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

    @Test
    void ddcAdminSpecUsesExplicitLocalDevelopmentSecurityMode() {
        topology.ddcAdminSpecUsesExplicitLocalDevelopmentSecurityMode();
    }

    @Test
    void gatewayAdminUsesInfrastructureRedisCoordinates() {
        topology.gatewayAdminUsesInfrastructureRedisCoordinates();
    }

    @Test
    void rpcConsumerEnablesDdcServiceRegistry() {
        topology.rpcConsumerEnablesDdcServiceRegistry();
    }
}
