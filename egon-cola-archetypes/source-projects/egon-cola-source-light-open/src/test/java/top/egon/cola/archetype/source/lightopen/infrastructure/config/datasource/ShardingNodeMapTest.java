package top.egon.cola.archetype.source.lightopen.infrastructure.config.datasource;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShardingNodeMapTest {
    private static final String NODE_MAP =
            "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1";

    @Test
    void parsesStableTwoByTwoTopology() {
        ShardingNodeMap map = parse("4", NODE_MAP);
        assertThat(map.nodes()).containsExactlyInAnyOrderEntriesOf(Map.of(
                0, new ShardingNodeMap.PhysicalNode("shard_0", 0),
                1, new ShardingNodeMap.PhysicalNode("shard_0", 1),
                2, new ShardingNodeMap.PhysicalNode("shard_1", 0),
                3, new ShardingNodeMap.PhysicalNode("shard_1", 1)));
    }

    @Test
    void routesPositiveTenantAndRejectsInvalidTenant() {
        ShardingNodeMap map = parse("4", NODE_MAP);
        assertThat(map.route(2001L)).isNotNull();
        assertThatThrownBy(() -> map.route(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> map.route(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidTopology() {
        assertThatThrownBy(() -> parse("3", "0=shard_0:0,1=shard_0:1,2=shard_1:0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ShardingNodeMap parse(String count, String nodes) {
        Properties properties = new Properties();
        properties.setProperty("node-count", count);
        properties.setProperty("node-map", nodes);
        return ShardingNodeMap.parse(properties);
    }
}
