package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShardingTopologyValidatorTest {

    @Test
    void shouldAcceptOnePrimaryTargetPerLogicalGroup() {
        assertThatCode(() -> new ShardingTopologyValidator()
                        .validate(validProperties(), validYaml()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectReplicaFlywayTarget() {
        ShardingDataSourceProperties valid = validProperties();
        List<ShardingDataSourceProperties.PhysicalDataSourceProperties> dataSources =
                new ArrayList<>(valid.physicalDataSources());
        dataSources.add(new ShardingDataSourceProperties.PhysicalDataSourceProperties(
                "shard_0_replica_0",
                "shard_0",
                ShardingDataSourceProperties.DataSourceRole.REPLICA,
                "org.h2.Driver",
                "jdbc:h2:mem:replica",
                "sa",
                "secret"));
        List<ShardingDataSourceProperties.FlywayTargetProperties> targets =
                new ArrayList<>(valid.flyway().targets());
        targets.add(new ShardingDataSourceProperties.FlywayTargetProperties(
                "shard_0_replica_0",
                List.of("classpath:db/shard")));
        ShardingDataSourceProperties invalid = new ShardingDataSourceProperties(
                valid.config(),
                valid.routing(),
                dataSources,
                new ShardingDataSourceProperties.ShardingFlywayProperties(targets));

        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(invalid, validYaml()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("replica");
    }

    @Test
    void shouldRejectMissingLogicalPrimary() {
        ShardingDataSourceProperties valid = validProperties();
        ShardingDataSourceProperties invalid = new ShardingDataSourceProperties(
                valid.config(),
                valid.routing(),
                valid.physicalDataSources().stream()
                        .filter(source -> !source.logicalName().equals("shard_1"))
                        .toList(),
                new ShardingDataSourceProperties.ShardingFlywayProperties(
                        valid.flyway().targets().stream()
                                .filter(target -> !target.dataSourceName().equals("shard_1"))
                                .toList()));

        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(invalid, validYaml()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shard_1");
    }

    static ShardingDataSourceProperties validProperties() {
        List<ShardingDataSourceProperties.PhysicalDataSourceProperties> sources = List.of(
                physical("single", "single"),
                physical("shard_0", "shard_0"),
                physical("shard_1", "shard_1"));
        List<ShardingDataSourceProperties.FlywayTargetProperties> targets = List.of(
                target("single", "single"),
                target("shard_0", "shard"),
                target("shard_1", "shard"));
        return new ShardingDataSourceProperties(
                "classpath:rules.yml",
                new ShardingDataSourceProperties.ShardingRoutingProperties(
                        1,
                        4,
                        "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1"),
                sources,
                new ShardingDataSourceProperties.ShardingFlywayProperties(targets));
    }

    static byte[] validYaml() {
        return """
                rules:
                  - !SHARDING
                    databaseAlgorithm:
                      mapping-version: 1
                      node-count: 4
                      node-map: 0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1
                    tableAlgorithm:
                      mapping-version: 1
                      node-count: 4
                      node-map: 0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1
                  - !SINGLE
                    defaultDataSource: single
                """.getBytes(StandardCharsets.UTF_8);
    }

    private static ShardingDataSourceProperties.PhysicalDataSourceProperties physical(
            String name,
            String logicalName) {
        return new ShardingDataSourceProperties.PhysicalDataSourceProperties(
                name,
                logicalName,
                ShardingDataSourceProperties.DataSourceRole.PRIMARY,
                "org.h2.Driver",
                "jdbc:h2:mem:" + name,
                "sa",
                "secret");
    }

    private static ShardingDataSourceProperties.FlywayTargetProperties target(
            String name,
            String location) {
        return new ShardingDataSourceProperties.FlywayTargetProperties(
                name,
                List.of("classpath:db/" + location));
    }
}
