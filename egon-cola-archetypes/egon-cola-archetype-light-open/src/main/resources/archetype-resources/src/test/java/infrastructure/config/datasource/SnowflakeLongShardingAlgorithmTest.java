package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Properties;
import org.apache.shardingsphere.infra.datanode.DataNodeInfo;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.junit.jupiter.api.Test;

class SnowflakeLongShardingAlgorithmTest {

    private static final long SHARDING_KEY = 42L;
    private static final DataNodeInfo DATA_NODE_INFO = new DataNodeInfo("records_", 1, '0');

    @Test
    void databaseAndTableStrategiesSelectTheSamePhysicalNode() {
        ShardingNodeMap nodeMap = nodeMap();
        ShardingNodeMap.PhysicalNode expected = nodeMap.route(SHARDING_KEY);
        SnowflakeLongShardingAlgorithm database = algorithm("database");
        SnowflakeLongShardingAlgorithm table = algorithm("table");

        assertThat(database.doSharding(
                List.of("shard_0", "shard_1"), precise(SHARDING_KEY)))
                .isEqualTo(expected.database());
        assertThat(table.doSharding(
                List.of("records_0", "records_1"), precise(SHARDING_KEY)))
                .isEqualTo("records_" + expected.tableSuffix());
    }

    @Test
    void rejectsInvalidKeyUnavailableTargetAndRangeRouting() {
        SnowflakeLongShardingAlgorithm database = algorithm("database");

        assertThatThrownBy(() -> database.doSharding(
                List.of("shard_0", "shard_1"),
                precise(0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> database.doSharding(
                List.of("other"), precise(SHARDING_KEY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target");
        assertThatThrownBy(() -> database.doSharding(
                List.of("shard_0", "shard_1"),
                new RangeShardingValue<>("records", "id", DATA_NODE_INFO, null)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("range");
    }

    private static SnowflakeLongShardingAlgorithm algorithm(String target) {
        Properties properties = properties();
        properties.setProperty("target", target);
        SnowflakeLongShardingAlgorithm result = new SnowflakeLongShardingAlgorithm();
        result.init(properties);
        return result;
    }

    private static ShardingNodeMap nodeMap() {
        return ShardingNodeMap.parse(properties());
    }

    private static Properties properties() {
        Properties properties = new Properties();
        properties.setProperty("node-count", "4");
        properties.setProperty(
                "node-map",
                "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1");
        return properties;
    }

    private static PreciseShardingValue<Long> precise(long value) {
        return new PreciseShardingValue<>("records", "id", DATA_NODE_INFO, value);
    }
}
