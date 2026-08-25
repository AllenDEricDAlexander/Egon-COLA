package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LightDataSourceModeTest {
    @Test
    void routes_positive_tenant_to_a_stable_slot() {
        ShardingNodeMap nodeMap = ShardingNodeMap.parse("4",
                "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1");
        assertThat(nodeMap.route(42L)).isEqualTo(nodeMap.route(42L));
        assertThat(nodeMap.route(42L).database()).startsWith("shard_");
    }

    @Test
    void keeps_manual_schema_outside_runtime_migration_resources() {
        assertThat(getClass().getClassLoader().getResource(
                "db/manual/postgresql/master-data/003__migrate_light_master_data_to_egon_model.sql"))
                .isNotNull();
        assertThat(getClass().getClassLoader().getResource(
                "db/migration/sharding/master-data/V20260726_001__init_light_master_data_schema.sql"))
                .isNull();
    }
}
