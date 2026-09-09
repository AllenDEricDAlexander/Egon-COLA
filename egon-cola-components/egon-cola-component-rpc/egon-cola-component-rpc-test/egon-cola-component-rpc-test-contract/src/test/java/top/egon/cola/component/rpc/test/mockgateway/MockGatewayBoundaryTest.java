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
                "top.egon.cola.component.rpc.yuheng.RpcGatewayNodeRegistrar"
        );
        assertMissing(
                "top.egon.cola.component.rpc.yuheng.RpcProviderDirectory"
        );
        assertMissing(
                "top.egon.cola.component.rpc.yuheng.RpcUnaryForwarder"
        );
    }

    private void assertMissing(String className) {
        assertThatThrownBy(() -> Class.forName(className))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
