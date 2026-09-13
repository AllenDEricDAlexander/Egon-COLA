package top.egon.cola.archetype.source.web.infrastructure.config.datasource;

import org.apache.shardingsphere.driver.yaml.YamlJDBCConfiguration;
import org.apache.shardingsphere.infra.datanode.DataNodeInfo;
import org.apache.shardingsphere.infra.util.yaml.YamlEngine;
import org.apache.shardingsphere.sharding.algorithm.sharding.classbased.ClassBasedShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import org.apache.shardingsphere.sharding.yaml.config.YamlShardingRuleConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ShardingRuleContractTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void typedYamlRoundTripKeepsSingleBroadcastBindingAndTheNativeClassBasedSpi() throws Exception {
        byte[] yaml;
        try (var input = new ClassPathResource("sharding/two-level-readwrite.yml").getInputStream()) { yaml = input.readAllBytes(); }
        var topology = ShardingTopologyValidatorTest.validated(ShardingTopologyValidatorTest.validReadwriteProperties(), yaml);
        assertThat(topology.profiles()).hasSize(4);
        assertThat(topology.broadcastTables()).containsExactly("routing_dictionary");
        var config = YamlEngine.unmarshal(new String(topology.yaml(), StandardCharsets.UTF_8), YamlJDBCConfiguration.class);
        var sharding = config.getRules().stream().filter(YamlShardingRuleConfiguration.class::isInstance)
                .map(YamlShardingRuleConfiguration.class::cast).findFirst().orElseThrow();
        assertThat(config.getTransaction().getDefaultType()).isEqualTo("LOCAL");
        assertThat(sharding.getBindingTables()).containsExactly("routing_order,routing_order_item");
        for (String table : List.of("routing_order", "routing_order_item")) {
            String algorithm = sharding.getTables().get(table).getDatabaseStrategy().getStandard().getShardingAlgorithmName();
            ClassBasedShardingAlgorithm nativeSpi = new ClassBasedShardingAlgorithm();
            nativeSpi.init(sharding.getShardingAlgorithms().get(algorithm).getProps());
            String selected = ((StandardShardingAlgorithm) nativeSpi).doSharding(List.of("shard_0", "shard_1"),
                    new PreciseShardingValue<>(table, "tenant_id", new DataNodeInfo("", 0, '0'), 41L));
            assertThat(selected).isEqualTo("shard_1");
        }
        assertThat(topology.profiles().get("routing_order").rootKeyName()).isEqualTo(topology.profiles().get("routing_order_item").rootKeyName());
    }

    @Test
    void bindingTablesCannotUseDifferentRootSemantics() throws Exception {
        String yaml;
        try (var input = new ClassPathResource("sharding/two-level-readwrite.yml").getInputStream()) { yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8); }
        String incompatible = yaml.replaceFirst("root-key-name: order", "root-key-name: item");
        assertThatThrownBy(() -> ShardingTopologyValidatorTest.validated(ShardingTopologyValidatorTest.validReadwriteProperties(),
                incompatible.getBytes(StandardCharsets.UTF_8))).hasMessageContaining("BINDING_POLICY_MISMATCH");
    }
}
