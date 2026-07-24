package ${package}.infrastructure.config.datasource;

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
        ShardingNodeMap nodeMap = parse("1", "4", INITIAL_NODE_MAP);

        assertThat(nodeMap.mappingVersion()).isEqualTo(1);
        assertThat(nodeMap.nodeCount()).isEqualTo(4);
        assertThat(nodeMap.nodes()).containsExactlyInAnyOrderEntriesOf(Map.of(
                0, new ShardingNodeMap.PhysicalNode("shard_0", 0),
                1, new ShardingNodeMap.PhysicalNode("shard_0", 1),
                2, new ShardingNodeMap.PhysicalNode("shard_1", 0),
                3, new ShardingNodeMap.PhysicalNode("shard_1", 1)));
    }

    @Test
    void rejectsInvalidTopology() {
        assertThatThrownBy(() -> parse("1", "3", "0=shard_0:0,1=shard_0:1,2=shard_1:0"))
                .isInstanceOf(IllegalArgumentException.class);

        Map<Integer, ShardingNodeMap.PhysicalNode> discontinuous = new LinkedHashMap<>();
        discontinuous.put(0, new ShardingNodeMap.PhysicalNode("shard_0", 0));
        discontinuous.put(1, new ShardingNodeMap.PhysicalNode("shard_0", 1));
        discontinuous.put(2, new ShardingNodeMap.PhysicalNode("shard_1", 0));
        discontinuous.put(4, new ShardingNodeMap.PhysicalNode("shard_1", 1));
        assertThatThrownBy(() -> new ShardingNodeMap(1, 4, discontinuous))
                .isInstanceOf(IllegalArgumentException.class);

        Map<Integer, ShardingNodeMap.PhysicalNode> duplicated = new LinkedHashMap<>();
        duplicated.put(0, new ShardingNodeMap.PhysicalNode("shard_0", 0));
        duplicated.put(1, new ShardingNodeMap.PhysicalNode("shard_0", 0));
        duplicated.put(2, new ShardingNodeMap.PhysicalNode("shard_1", 0));
        duplicated.put(3, new ShardingNodeMap.PhysicalNode("shard_1", 1));
        assertThatThrownBy(() -> new ShardingNodeMap(1, 4, duplicated))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> parse("1", "4",
                "0=shard_0:0,1=shard_0:1,2=shard_0:2,3=shard_1:0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doublesSlotsWithoutRemappingExistingBucket() {
        ShardingNodeMap current = parse("1", "4", INITIAL_NODE_MAP);
        ShardingNodeMap expanded = parse("2", "8",
                INITIAL_NODE_MAP
                        + ",4=shard_2:0,5=shard_2:1,6=shard_3:0,7=shard_3:1");

        List<String> keys = List.of(
                "018f5f9c-4f6a-7c2b-8a1d-123456789abc",
                "018f5f9c-4f6b-7c2b-8a1d-123456789abd",
                "018f5f9c-4f6c-7c2b-8a1d-123456789abe",
                "018f5f9c-4f6d-7c2b-8a1d-123456789abf");

        keys.forEach(key -> assertThat(expanded.routeSlot(key))
                .isIn(current.routeSlot(key), current.routeSlot(key) + current.nodeCount()));
    }

    @Test
    void acceptsOnlyUuidV7ShardingKeys() {
        ShardingNodeMap nodeMap = parse("1", "4", INITIAL_NODE_MAP);

        assertThatThrownBy(() -> nodeMap.route(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUIDv7");
        assertThatThrownBy(() -> nodeMap.route(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUIDv7");
        assertThatThrownBy(() -> nodeMap.route("550e8400-e29b-41d4-a716-446655440000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUIDv7");
    }

    private static ShardingNodeMap parse(String mappingVersion, String nodeCount, String nodes) {
        Properties properties = new Properties();
        properties.setProperty("mapping-version", mappingVersion);
        properties.setProperty("node-count", nodeCount);
        properties.setProperty("node-map", nodes);
        return ShardingNodeMap.parse(properties);
    }
}
