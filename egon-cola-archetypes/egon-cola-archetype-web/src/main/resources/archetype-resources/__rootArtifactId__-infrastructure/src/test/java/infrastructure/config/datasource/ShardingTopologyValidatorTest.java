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

    @Test
    void shouldRejectReadwriteGroupWhoseWriterIsReplica() {
        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(validReadwriteProperties(), readwriteYaml("single_replica_0")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("write data source");
    }

    @Test
    void shouldRejectActualDataNodesOutsideStableNodeMap() {
        byte[] invalid = new String(validYaml(), StandardCharsets.UTF_8)
                .replace("shard_$->{0..1}", "shard_$->{0..2}")
                .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(validProperties(), invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actualDataNodes");
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
                    tables:
                      sample:
                        actualDataNodes: shard_$->{0..1}.public.sample_$->{0..1}
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

    private static ShardingDataSourceProperties validReadwriteProperties() {
        List<ShardingDataSourceProperties.PhysicalDataSourceProperties> sources = List.of(
                physical("single_primary", "single"),
                replica("single_replica_0", "single"),
                physical("shard_0_primary", "shard_0"),
                replica("shard_0_replica_0", "shard_0"),
                physical("shard_1_primary", "shard_1"),
                replica("shard_1_replica_0", "shard_1"));
        List<ShardingDataSourceProperties.FlywayTargetProperties> targets = List.of(
                target("single_primary", "single"),
                target("shard_0_primary", "shard"),
                target("shard_1_primary", "shard"));
        return new ShardingDataSourceProperties(
                "classpath:rules.yml",
                validProperties().routing(),
                sources,
                new ShardingDataSourceProperties.ShardingFlywayProperties(targets));
    }

    private static byte[] readwriteYaml(String singleWriter) {
        return ("""
                rules:
                  - !READWRITE_SPLITTING
                    dataSourceGroups:
                      single:
                        writeDataSourceName: %s
                        readDataSourceNames:
                          - single_replica_0
                        transactionalReadQueryStrategy: PRIMARY
                        loadBalancerName: round_robin
                      shard_0:
                        writeDataSourceName: shard_0_primary
                        readDataSourceNames:
                          - shard_0_replica_0
                        transactionalReadQueryStrategy: PRIMARY
                        loadBalancerName: round_robin
                      shard_1:
                        writeDataSourceName: shard_1_primary
                        readDataSourceNames:
                          - shard_1_replica_0
                        transactionalReadQueryStrategy: PRIMARY
                        loadBalancerName: round_robin
                    loadBalancers:
                      round_robin:
                        type: ROUND_ROBIN
                  - !SHARDING
                    tables:
                      sample:
                        actualDataNodes: shard_$->{0..1}.public.sample_$->{0..1}
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
                """).formatted(singleWriter).getBytes(StandardCharsets.UTF_8);
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

    private static ShardingDataSourceProperties.PhysicalDataSourceProperties replica(
            String name,
            String logicalName) {
        return new ShardingDataSourceProperties.PhysicalDataSourceProperties(
                name,
                logicalName,
                ShardingDataSourceProperties.DataSourceRole.REPLICA,
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
