package top.egon.cola.component.rpc.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RpcRuntimeVersionTest {

    @Test
    void shouldLoadFilteredRuntimeVersion() {
        assertThat(RpcRuntimeVersion.load())
                .matches("\\d+\\.\\d+\\.\\d+")
                .doesNotContain("${");
    }
}
