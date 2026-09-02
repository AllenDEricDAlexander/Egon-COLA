package top.egon.cola.archetype.source.web.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class ShardingNodeMapTest {

    private static final String INITIAL_NODE_MAP =
            "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1";

    @Test
    void parsesInitialTwoByTwoTopologyExactly() {
        ShardingNodeMap nodeMap = parse("4", INITIAL_NODE_MAP);

        assertThat(nodeMap.nodeCount()).isEqualTo(4);
        assertThat(nodeMap.nodes()).containsExactlyInAnyOrderEntriesOf(Map.of(
                0, new ShardingNodeMap.PhysicalNode("shard_0", 0),
                1, new ShardingNodeMap.PhysicalNode("shard_0", 1),
                2, new ShardingNodeMap.PhysicalNode("shard_1", 0),
                3, new ShardingNodeMap.PhysicalNode("shard_1", 1)));
    }

    @Test
    void rejectsInvalidTopology() {
        assertThatThrownBy(() -> parse("3", "0=shard_0:0,1=shard_0:1,2=shard_1:0"))
                .isInstanceOf(IllegalArgumentException.class);

        Map<Integer, ShardingNodeMap.PhysicalNode> discontinuous = new LinkedHashMap<>();
        discontinuous.put(0, new ShardingNodeMap.PhysicalNode("shard_0", 0));
        discontinuous.put(1, new ShardingNodeMap.PhysicalNode("shard_0", 1));
        discontinuous.put(2, new ShardingNodeMap.PhysicalNode("shard_1", 0));
        discontinuous.put(4, new ShardingNodeMap.PhysicalNode("shard_1", 1));
        assertThatThrownBy(() -> new ShardingNodeMap(4, discontinuous))
                .isInstanceOf(IllegalArgumentException.class);

        Map<Integer, ShardingNodeMap.PhysicalNode> duplicated = new LinkedHashMap<>();
        duplicated.put(0, new ShardingNodeMap.PhysicalNode("shard_0", 0));
        duplicated.put(1, new ShardingNodeMap.PhysicalNode("shard_0", 0));
        duplicated.put(2, new ShardingNodeMap.PhysicalNode("shard_1", 0));
        duplicated.put(3, new ShardingNodeMap.PhysicalNode("shard_1", 1));
        assertThatThrownBy(() -> new ShardingNodeMap(4, duplicated))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> parse("4",
                "0=shard_0:0,1=shard_0:1,2=shard_0:2,3=shard_1:0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doublesSlotsWithoutMovingAKeyOutsideItsOriginalBucketPair() {
        ShardingNodeMap current = parse("4", INITIAL_NODE_MAP);
        ShardingNodeMap expanded = parse("8",
                INITIAL_NODE_MAP
                        + ",4=shard_2:0,5=shard_2:1,6=shard_3:0,7=shard_3:1");

        List<Long> keys = List.of(1001L, 1002L, 1003L, 1004L);

        keys.forEach(key -> assertThat(expanded.routeSlot(key))
                .isIn(current.routeSlot(key), current.routeSlot(key) + current.nodeCount()));
    }

    @Test
    void acceptsOnlyPositiveLongShardingKeys() {
        ShardingNodeMap nodeMap = parse("4", INITIAL_NODE_MAP);

        assertThatThrownBy(() -> nodeMap.route(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> nodeMap.route(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    private static ShardingNodeMap parse(String nodeCount, String nodes) {
        Properties properties = new Properties();
        properties.setProperty("node-count", nodeCount);
        properties.setProperty("node-map", nodes);
        return ShardingNodeMap.parse(properties);
    }
}
