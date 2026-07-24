package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Properties;
import org.apache.shardingsphere.infra.datanode.DataNodeInfo;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.junit.jupiter.api.Test;

class UuidV7BucketShardingAlgorithmTest {

    private static final String UUID_V7 = "018f5f9c-4f6a-7c2b-8a1d-123456789abc";
    private static final DataNodeInfo DATA_NODE_INFO = new DataNodeInfo("records_", 1, '0');

    @Test
    void databaseAndTableStrategiesSelectTheSamePhysicalNode() {
        ShardingNodeMap nodeMap = nodeMap();
        ShardingNodeMap.PhysicalNode expected = nodeMap.route(UUID_V7);
        UuidV7BucketShardingAlgorithm database = algorithm("database");
        UuidV7BucketShardingAlgorithm table = algorithm("table");

        assertThat(database.doSharding(
                List.of("shard_0", "shard_1"), precise(UUID_V7)))
                .isEqualTo(expected.database());
        assertThat(table.doSharding(
                List.of("records_0", "records_1"), precise(UUID_V7)))
                .isEqualTo("records_" + expected.tableSuffix());
    }

    @Test
    void rejectsInvalidKeyUnavailableTargetAndRangeRouting() {
        UuidV7BucketShardingAlgorithm database = algorithm("database");

        assertThatThrownBy(() -> database.doSharding(
                List.of("shard_0", "shard_1"),
                precise("550e8400-e29b-41d4-a716-446655440000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUIDv7");
        assertThatThrownBy(() -> database.doSharding(
                List.of("other"), precise(UUID_V7)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target");
        assertThatThrownBy(() -> database.doSharding(
                List.of("shard_0", "shard_1"),
                new RangeShardingValue<>("records", "id", DATA_NODE_INFO, null)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("range");
    }

    private static UuidV7BucketShardingAlgorithm algorithm(String target) {
        Properties properties = properties();
        properties.setProperty("target", target);
        UuidV7BucketShardingAlgorithm result = new UuidV7BucketShardingAlgorithm();
        result.init(properties);
        return result;
    }

    private static ShardingNodeMap nodeMap() {
        return ShardingNodeMap.parse(properties());
    }

    private static Properties properties() {
        Properties properties = new Properties();
        properties.setProperty("mapping-version", "1");
        properties.setProperty("node-count", "4");
        properties.setProperty(
                "node-map",
                "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1");
        return properties;
    }

    private static PreciseShardingValue<String> precise(String value) {
        return new PreciseShardingValue<>("records", "id", DATA_NODE_INFO, value);
    }
}
