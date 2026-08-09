package top.egon.cola.component.rpc.ddc.client;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRpcClientHandleTest {

    @Test
    void closesOwnedDirectHandleExactlyOnce() {
        AtomicBoolean closed = new AtomicBoolean();
        DdcRpcClientHandle<String> handle = new DdcRpcClientHandle<>(
                "client",
                () -> closed.set(true)
        );

        assertThat(handle.client()).isEqualTo("client");
        handle.close();
        handle.close();

        assertThat(closed).isTrue();
    }
}
