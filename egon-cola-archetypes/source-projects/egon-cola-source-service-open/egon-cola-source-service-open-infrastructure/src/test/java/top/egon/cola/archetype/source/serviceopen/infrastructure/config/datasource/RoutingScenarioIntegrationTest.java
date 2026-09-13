package top.egon.cola.archetype.source.serviceopen.infrastructure.config.datasource;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery.OperationEnum.*;

/** Exercises the host adapter and common strategy against the actual typed YAML contract. */
class RoutingScenarioIntegrationTest {
    @Test
    void sameRootRoutesOrderAndItemsTogetherAndBoundsRangeReads() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory();
             var input = new ClassPathResource("sharding/two-level-readwrite.yml").getInputStream()) {
            var validation = new ValidationUtils(factory.getValidator());
            var topology = ShardingTopologyValidatorTest.validated(
                    ShardingTopologyValidatorTest.validReadwriteProperties(), input.readAllBytes());
            var resolver = new ShardingWriteTargetResolver(topology.profiles(), topology.legacy(),
                    new EgonColaTwoLevelRouteStrategy(validation), validation, topology.fingerprint());
            var order = resolver.resolve(new EgonColaRouteQuery("routing_order", 41L, List.of(1L), COMMAND, false));
            var item = resolver.resolve(new EgonColaRouteQuery("routing_order_item", 41L, List.of(1L), COMMAND, false));
            assertThat(order.targets()).hasSize(1);
            assertThat(item.targets().getFirst().group()).isEqualTo(order.targets().getFirst().group());
            assertThat(item.targets().getFirst().table()).isEqualTo(order.targets().getFirst().table().replace("order_", "order_item_"));
            assertThat(resolver.resolve(new EgonColaRouteQuery("routing_order", 41L, List.of(), QUERY, true)).targets())
                    .hasSize(2).allSatisfy(target -> assertThat(target.group()).isEqualTo("shard_1"));
            assertThatThrownBy(() -> resolver.resolve(new EgonColaRouteQuery("routing_order", 41L, List.of(), COMMAND, false)))
                    .hasMessageContaining("command requires exact");
            assertThatThrownBy(() -> resolver.resolve(new EgonColaRouteQuery("routing_order", 0L, List.of(1L), QUERY, false)))
                    .hasMessageContaining("positive Long");
            assertThat(resolver.resolve(new EgonColaRouteQuery("routing_metadata", 41L, List.of(), QUERY, false)).targets())
                    .singleElement().satisfies(target -> assertThat(target.group()).isEqualTo("master_data"));
            assertThat(resolver.resolve(new EgonColaRouteQuery("routing_dictionary", 41L, List.of(), QUERY, false)).targets()).hasSize(3);
            assertThatThrownBy(() -> resolver.resolve(new EgonColaRouteQuery("routing_dictionary", 41L, List.of(1L), COMMAND, false)))
                    .hasMessageContaining("BROADCAST_READ_ONLY");
        }
    }

    @Test
    void legacyAdapterPreservesTheExistingTenantAddress() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validation = new ValidationUtils(factory.getValidator());
            var topology = ShardingTopologyValidatorTest.validated(
                    ShardingTopologyValidatorTest.validProperties(), ShardingTopologyValidatorTest.yaml(false));
            var resolver = new ShardingWriteTargetResolver(topology.profiles(), topology.legacy(),
                    new EgonColaTwoLevelRouteStrategy(validation), validation, topology.fingerprint());
            assertThat(resolver.resolve(new EgonColaRouteQuery("evaluation_exam", 41L, List.of(1L), COMMAND, false)).targets())
                    .singleElement().satisfies(target -> {
                        assertThat(target.group()).isEqualTo("shard_0");
                        assertThat(target.table()).isEqualTo("evaluation_exam_1");
                    });
        }
    }
    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "egon.pg.routing", matches = "true")
    void nativeShardingSphereWritesOnlyTheCalculatedPhysicalTables() throws Exception {
        String url = System.getenv("EGON_TEST_PG_URL");
        assertThat(url).as("explicit disposable PostgreSQL test database").startsWith("jdbc:postgresql:").doesNotContain("currentSchema=");
        String username = System.getenv("EGON_TEST_PG_USER");
        String password = System.getenv("EGON_TEST_PG_PASSWORD");
        assertThat(username).isNotBlank();
        assertThat(password).isNotNull();
        var physical = new java.util.LinkedHashMap<String, javax.sql.DataSource>();
        var schemas = new java.util.LinkedHashMap<String, String>();
        var admin = new org.springframework.jdbc.datasource.DriverManagerDataSource(url, username, password);
        javax.sql.DataSource logical = null;
        try {
            for (String group : List.of("master_data", "shard_0", "shard_1")) {
                String schema = "egon_route_" + java.util.UUID.randomUUID().toString().replace("-", "");
                try (var connection = admin.getConnection(); var statement = connection.createStatement()) {
                    statement.execute("CREATE SCHEMA " + schema);
                    schemas.put(group, schema);
                    connection.setSchema(schema);
                    org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                            new org.springframework.core.io.support.EncodedResource(new ClassPathResource("sharding/pgsql-routing-fixture.sql"), java.nio.charset.StandardCharsets.UTF_8), false, false,
                            "--", org.springframework.jdbc.datasource.init.ScriptUtils.EOF_STATEMENT_SEPARATOR, "/*", "*/");
                }
                physical.put(group, new org.springframework.jdbc.datasource.DriverManagerDataSource(
                        url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, username, password));
            }
            byte[] inputYaml;
            try (var input = new ClassPathResource("sharding/two-level-readwrite.yml").getInputStream()) { inputYaml = input.readAllBytes(); }
            var config = org.apache.shardingsphere.infra.util.yaml.YamlEngine.unmarshal(
                    new String(inputYaml, java.nio.charset.StandardCharsets.UTF_8), org.apache.shardingsphere.driver.yaml.YamlJDBCConfiguration.class);
            config.getRules().removeIf(org.apache.shardingsphere.readwritesplitting.yaml.config.YamlReadwriteSplittingRuleConfiguration.class::isInstance);
            config.getRules().stream().filter(org.apache.shardingsphere.single.yaml.config.YamlSingleRuleConfiguration.class::isInstance)
                    .map(org.apache.shardingsphere.single.yaml.config.YamlSingleRuleConfiguration.class::cast)
                    .forEach(single -> single.setTables(List.of("master_data." + schemas.get("master_data") + ".routing_metadata")));
            var base = ShardingTopologyValidatorTest.validProperties();
            var targets = base.ddl().targets().stream().map(target -> new ShardingDataSourceProperties.DdlTargetProperties(
                    target.dataSourceName(), schemas.get(target.dataSourceName()), target.role(), target.manifest())).toList();
            var properties = new ShardingDataSourceProperties(base.config(), base.routing(), base.physicalDataSources(),
                    new ShardingDataSourceProperties.ShardingDdlProperties(targets));
            var topology = ShardingTopologyValidatorTest.validated(properties,
                    org.apache.shardingsphere.infra.util.yaml.YamlEngine.marshal(config).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            logical = org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory.createDataSource(physical, topology.yaml());
            try (var connection = logical.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    for (String table : List.of("routing_order", "routing_order_item")) {
                        try (var statement = connection.prepareStatement("INSERT INTO " + table
                                + " (id,tenant_id,create_user_id,update_user_id,create_time,update_time,deleted_at,version,order_id,payload)"
                                + " VALUES (?,41,'test','test',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL,0,1,'placed')")) {
                            statement.setLong(1, table.equals("routing_order") ? 1 : 11);
                            assertThat(statement.executeUpdate()).isEqualTo(1);
                        }
                    }
                    connection.commit();
                } catch (java.sql.SQLException failure) { connection.rollback(); throw failure; }
            }
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                var strategy = new EgonColaTwoLevelRouteStrategy(new ValidationUtils(factory.getValidator()));
                for (String table : List.of("routing_order", "routing_order_item")) {
                    var profile = topology.profiles().get(table);
                    var expected = strategy.route(profile, new EgonColaRouteQuery(table, 41L, List.of(1L), COMMAND, false)).targets().getFirst();
                    for (var nodes : profile.actualNodes().values()) {
                        for (var node : nodes) {
                            try (var connection = physical.get(node.group()).getConnection(); var statement = connection.createStatement();
                                 var rows = statement.executeQuery("SELECT count(*) FROM " + node.table() + " WHERE tenant_id=41")) {
                                rows.next();
                                assertThat(rows.getLong(1)).as(node.toString()).isEqualTo(node.equals(expected) ? 1 : 0);
                            }
                        }
                    }
                }
            }
            try (var connection = logical.getConnection(); var statement = connection.prepareStatement(
                    "SELECT id FROM routing_order WHERE tenant_id=? AND deleted_at IS NULL")) {
                statement.setLong(1, 41);
                try (var rows = statement.executeQuery()) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong(1)).isEqualTo(1);
                    assertThat(rows.next()).isFalse();
                }
            }
        } finally {
            try { if (logical instanceof AutoCloseable closeable) { closeable.close(); } }
            finally {
                for (String schema : schemas.values()) {
                    try (var connection = admin.getConnection(); var statement = connection.createStatement()) {
                        statement.execute("DROP SCHEMA " + schema + " CASCADE");
                    }
                }
            }
        }
    }
}
