package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShardingTopologyValidatorTest {

    @Test
    void shouldAcceptNoneMasterDataAndAuditedShardedTables() {
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
                        .validate(
                                validReadwriteProperties(),
                                readwriteYaml("master_data_replica_0")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("write data source");
    }

    @Test
    void shouldRejectActualDataNodesOutsideStableNodeMap() {
        byte[] invalid = replace(
                validYaml(),
                "shard_$->{0..1}",
                "shard_$->{0..2}");

        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(validProperties(), invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actualDataNodes");
    }

    @Test
    void shouldRejectActualDataNodesWhosePhysicalTableDoesNotMatchLogicalTable() {
        byte[] invalid = replace(
                validYaml(),
                "sample_$->{0..1}",
                "another_table_$->{0..1}");

        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(validProperties(), invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("physical table");
    }

    @Test
    void shouldRejectRemovedSingleRule() {
        byte[] invalid = (new String(validYaml(), StandardCharsets.UTF_8) + """

                  - !SINGLE
                    tables:
                      - master_data.public.legacy_table
                    defaultDataSource: master_data
                """).getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(validProperties(), invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("!SINGLE");
    }

    @Test
    void shouldRejectMasterDataTableWithoutBothNoneStrategies() {
        byte[] invalid = replace(
                validYaml(),
                """
                        databaseStrategy:
                          none:
                        tableStrategy:
                          none:
                """,
                """
                        databaseStrategy:
                          standard:
                        tableStrategy:
                          none:
                """);

        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(validProperties(), invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("none");
    }

    @Test
    void shouldRejectShardedTableWithoutDmlAudit() {
        byte[] invalid = replace(
                validYaml(),
                """
                        auditStrategy:
                          auditorNames:
                            - sharding_key_required_auditor
                          allowHintDisable: false
                """,
                "");

        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(validProperties(), invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audit");
    }

    @Test
    void shouldRejectHintDisableBypassForShardedDml() {
        byte[] invalid = replace(
                validYaml(),
                "allowHintDisable: false",
                "allowHintDisable: true");

        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(validProperties(), invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allowHintDisable");
    }

    @Test
    void shouldRejectRoutingScalarWhoseValueOnlySharesExpectedPrefix() {
        byte[] invalid = replace(validYaml(), "node-count: 4", "node-count: 40");

        assertThatThrownBy(() -> new ShardingTopologyValidator()
                        .validate(validProperties(), invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routing property");
    }

    static ShardingDataSourceProperties validProperties() {
        List<ShardingDataSourceProperties.PhysicalDataSourceProperties> sources = List.of(
                physical("master_data", "master_data"),
                physical("shard_0", "shard_0"),
                physical("shard_1", "shard_1"));
        List<ShardingDataSourceProperties.FlywayTargetProperties> targets = List.of(
                target("master_data", "master-data"),
                target("shard_0", "shard"),
                target("shard_1", "shard"));
        return new ShardingDataSourceProperties(
                "classpath:rules.yml",
                new ShardingDataSourceProperties.ShardingRoutingProperties(
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
                      users:
                        actualDataNodes: master_data.public.users
                        databaseStrategy:
                          none:
                        tableStrategy:
                          none:
                      sample:
                        actualDataNodes: shard_$->{0..1}.public.sample_$->{0..1}
                        databaseStrategy:
                          standard:
                            shardingColumn: id
                            shardingAlgorithmName: uuid_v7_database_bucket
                        tableStrategy:
                          standard:
                            shardingColumn: id
                            shardingAlgorithmName: uuid_v7_table_bucket
                        auditStrategy:
                          auditorNames:
                            - sharding_key_required_auditor
                          allowHintDisable: false
                    shardingAlgorithms:
                      uuid_v7_database_bucket:
                        type: CLASS_BASED
                        props:
                          node-count: 4
                          node-map: 0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1
                      uuid_v7_table_bucket:
                        type: CLASS_BASED
                        props:
                          node-count: 4
                          node-map: 0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1
                    auditors:
                      sharding_key_required_auditor:
                        type: DML_SHARDING_CONDITIONS
                """.getBytes(StandardCharsets.UTF_8);
    }

    private static ShardingDataSourceProperties validReadwriteProperties() {
        List<ShardingDataSourceProperties.PhysicalDataSourceProperties> sources = List.of(
                physical("master_data_primary", "master_data"),
                replica("master_data_replica_0", "master_data"),
                physical("shard_0_primary", "shard_0"),
                replica("shard_0_replica_0", "shard_0"),
                physical("shard_1_primary", "shard_1"),
                replica("shard_1_replica_0", "shard_1"));
        List<ShardingDataSourceProperties.FlywayTargetProperties> targets = List.of(
                target("master_data_primary", "master-data"),
                target("shard_0_primary", "shard"),
                target("shard_1_primary", "shard"));
        return new ShardingDataSourceProperties(
                "classpath:rules.yml",
                validProperties().routing(),
                sources,
                new ShardingDataSourceProperties.ShardingFlywayProperties(targets));
    }

    private static byte[] readwriteYaml(String masterDataWriter) {
        String shardingRules = new String(validYaml(), StandardCharsets.UTF_8)
                .replaceFirst("rules:\\R", "");
        return ("""
                rules:
                  - !READWRITE_SPLITTING
                    dataSourceGroups:
                      master_data:
                        writeDataSourceName: %s
                        readDataSourceNames:
                          - master_data_replica_0
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
                """.formatted(masterDataWriter) + shardingRules)
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] replace(byte[] source, String target, String replacement) {
        return new String(source, StandardCharsets.UTF_8)
                .replace(target, replacement)
                .getBytes(StandardCharsets.UTF_8);
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
                List.of("classpath:db/migration/sharding/" + location));
    }
}
