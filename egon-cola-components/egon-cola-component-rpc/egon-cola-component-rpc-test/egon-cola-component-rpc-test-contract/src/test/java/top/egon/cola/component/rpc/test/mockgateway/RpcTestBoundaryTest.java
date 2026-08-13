package top.egon.cola.component.rpc.test.mockgateway;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.consumer.channel.RpcConsumerChannelFactory;
import top.egon.cola.component.rpc.provider.server.RpcProviderServerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class RpcTestBoundaryTest {

    @Test
    void shouldUseNettyTcpAndKeepMockGatewayInTestScope() {
        assertThat(RpcProviderServerFactory.class.getName())
                .doesNotContain("InProcess");
        assertThat(RpcConsumerChannelFactory.class.getName())
                .doesNotContain("InProcess");
        assertThat(MockRpcGateway.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toString()).contains("test-classes");
    }
}
