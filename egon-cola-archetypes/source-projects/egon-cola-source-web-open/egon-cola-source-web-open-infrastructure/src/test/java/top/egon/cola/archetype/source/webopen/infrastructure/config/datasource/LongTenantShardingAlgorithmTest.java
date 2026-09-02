package top.egon.cola.archetype.source.webopen.infrastructure.config.datasource;

import org.apache.shardingsphere.infra.datanode.DataNodeInfo;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LongTenantShardingAlgorithmTest {
    @Test
    void routesDatabaseAndTableUsingTheSameTenantSlot() {
        LongTenantShardingAlgorithm database = algorithm("database");
        LongTenantShardingAlgorithm table = algorithm("table");
        PreciseShardingValue<Long> value = precise(2001L);
        String databaseTarget = database.doSharding(List.of("shard_0", "shard_1"), value);
        String tableTarget = table.doSharding(List.of("records_0", "records_1"), value);
        assertThat(databaseTarget).startsWith("shard_");
        assertThat(tableTarget).matches("records_[01]");
    }

    @Test
    void rejectsNonPositiveTenant() {
        LongTenantShardingAlgorithm algorithm = algorithm("database");
        assertThatThrownBy(() -> algorithm.doSharding(
                List.of("shard_0", "shard_1"), precise(0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    private static LongTenantShardingAlgorithm algorithm(String target) {
        LongTenantShardingAlgorithm algorithm = new LongTenantShardingAlgorithm();
        Properties properties = new Properties();
        properties.setProperty("node-count", "4");
        properties.setProperty("node-map", "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1");
        properties.setProperty("target", target);
        algorithm.init(properties);
        return algorithm;
    }

    private static PreciseShardingValue<Long> precise(Long tenantId) {
        return new PreciseShardingValue<>(
                "tenant_id", "records", new DataNodeInfo("", 0, '0'), tenantId);
    }
}
