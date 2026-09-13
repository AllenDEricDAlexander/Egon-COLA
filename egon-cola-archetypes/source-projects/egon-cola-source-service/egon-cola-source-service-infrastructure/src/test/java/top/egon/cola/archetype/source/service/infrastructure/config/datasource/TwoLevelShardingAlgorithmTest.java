package top.egon.cola.archetype.source.service.infrastructure.config.datasource;

import org.apache.shardingsphere.infra.datanode.DataNodeInfo;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

class TwoLevelShardingAlgorithmTest {

    @Test
    void databaseAndTableCallbacksUseTheSameFixedTenantAndRootPolicy() {
        var database = new TenantDatabaseShardingAlgorithm();
        var table = new TenantBusinessTableShardingAlgorithm();
        Properties props = properties();
        database.init(props);
        table.init(props);
        var tenant = new PreciseShardingValue<>("routing_order", "tenant_id", new DataNodeInfo("", 0, '0'), 41L);
        assertThat(database.doSharding(List.of("shard_0", "shard_1"), tenant)).isEqualTo("shard_1");
        Map<String, Collection<Long>> keys = Map.of("tenant_id", List.of(41L), "id", List.of(1L));
        assertThat(table.doSharding(tables(), new ComplexKeysShardingValue<>("routing_order", keys, Map.of())))
                .containsExactly("routing_order_t5_b0");
        assertThat(table.doSharding(tables(), new ComplexKeysShardingValue<>("routing_order", Map.of("tenant_id", List.of(41L)), Map.of())))
                .hasSize(8).allSatisfy(name -> assertThat(name).startsWith("routing_order_t5_"));
    }

    @Test
    void callbacksRejectMissingOrMultipleTenantsAndUnknownTargets() {
        var table = new TenantBusinessTableShardingAlgorithm();
        table.init(properties());
        assertThatThrownBy(() -> table.doSharding(tables(), new ComplexKeysShardingValue<>("routing_order", Map.of("id", List.of(1L)), Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.doSharding(tables(), new ComplexKeysShardingValue<>("routing_order", Map.of("tenant_id", List.of(1L, 2L)), Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.doSharding(List.of("not_a_node"), new ComplexKeysShardingValue<>("routing_order", Map.of("tenant_id", List.of(41L), "id", List.of(1L)), Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Properties properties() {
        Properties props = new Properties();
        props.setProperty("logical-table", "routing_order");
        props.setProperty("algorithm-version", "mix64-v1");
        props.setProperty("tenant-slot-count", "16");
        props.setProperty("secondary-bucket-count", "8");
        props.setProperty("tenant-slot-map", java.util.stream.IntStream.range(0, 16)
                .mapToObj(slot -> slot + "=shard_" + slot % 2).collect(java.util.stream.Collectors.joining(",")));
        props.setProperty("secondary-column", "id");
        props.setProperty("root-key-name", "order");
        props.setProperty("secondary-seed", "0x9e3779b97f4a7c15");
        props.setProperty("max-read-fanout-tables", "8");
        props.setProperty("schema", "public");
        props.setProperty("actual-data-nodes", java.util.stream.IntStream.range(0, 16).boxed().flatMap(slot ->
                java.util.stream.IntStream.range(0, 8).mapToObj(bucket -> "shard_" + slot % 2 + ".public.routing_order_t" + slot + "_b" + bucket))
                .collect(java.util.stream.Collectors.joining(",")));
        return props;
    }

    private static List<String> tables() {
        List<String> result = new ArrayList<>();
        for (int t = 0; t < 16; t++) for (int b = 0; b < 8; b++) result.add("routing_order_t" + t + "_b" + b);
        return result;
    }
}
