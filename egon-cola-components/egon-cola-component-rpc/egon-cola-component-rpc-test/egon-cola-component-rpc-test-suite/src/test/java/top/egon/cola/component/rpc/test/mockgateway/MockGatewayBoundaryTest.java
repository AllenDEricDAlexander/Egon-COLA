package top.egon.cola.component.rpc.test.mockgateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockGatewayBoundaryTest {

    @Test
    void shouldKeepMockTypesInTestOutputAndProductionTypesAbsent() {
        assertThat(MockRpcGateway.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toString()).contains("test-classes");

        assertMissing(
                "top.egon.cola.component.rpc.gateway.RpcGatewayNodeRegistrar"
        );
        assertMissing(
                "top.egon.cola.component.rpc.gateway.RpcProviderDirectory"
        );
        assertMissing(
                "top.egon.cola.component.rpc.gateway.RpcUnaryForwarder"
        );
    }

    private void assertMissing(String className) {
        assertThatThrownBy(() -> Class.forName(className))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
