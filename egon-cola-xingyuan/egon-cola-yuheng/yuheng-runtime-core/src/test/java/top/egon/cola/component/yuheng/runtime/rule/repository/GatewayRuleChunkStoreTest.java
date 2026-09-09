package top.egon.cola.component.yuheng.runtime.rule.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRuleChunkStoreTest {

    @Test
    void removesChunkWhenDdcDeletesTheConfigurationLeaf() {
        GatewayRuleChunkStore store = new GatewayRuleChunkStore();
        String key = "gateway.rules.chunk.release-1.0";

        store.apply(key, "Y2h1bms=", 1L);
        assertThat(store.size()).isEqualTo(1);

        store.apply(key, null, 2L);

        assertThat(store.size()).isZero();
    }
}
