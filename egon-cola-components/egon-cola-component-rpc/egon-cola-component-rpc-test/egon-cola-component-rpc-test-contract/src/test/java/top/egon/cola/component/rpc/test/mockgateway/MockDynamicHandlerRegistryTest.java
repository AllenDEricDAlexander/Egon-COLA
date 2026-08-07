package top.egon.cola.component.rpc.test.mockgateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockDynamicHandlerRegistryTest {

    @Test
    void shouldRetainOriginalMethodNameAndRejectInvalidName() {
        MockDynamicHandlerRegistry registry =
                new MockDynamicHandlerRegistry();
        registry.register(
                "egon.rpc.test.v1.EchoService/Echo",
                (request, observer) -> {
                }
        );

        assertThat(registry.lookupMethod(
                "egon.rpc.test.v1.EchoService/Echo",
                null
        ).getMethodDescriptor().getFullMethodName())
                .isEqualTo("egon.rpc.test.v1.EchoService/Echo");
        assertThatThrownBy(() -> registry.register(
                "invalid",
                (request, observer) -> {
                }
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
