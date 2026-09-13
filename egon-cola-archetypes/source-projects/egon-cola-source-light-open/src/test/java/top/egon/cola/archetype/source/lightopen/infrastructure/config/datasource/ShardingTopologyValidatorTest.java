package top.egon.cola.archetype.source.lightopen.infrastructure.config.datasource;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ShardingTopologyValidatorTest {
    private ValidatorFactory factory;
    private ShardingTopologyValidator validator;

    @BeforeEach void prepare() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = new ShardingTopologyValidator(new ValidationUtils(factory.getValidator()));
    }
    @AfterEach void close() { factory.close(); }

    @Test
    void rejectsReadOnlyPrimaryEvenWhenItIsNotAReplica() throws Exception {
        var properties = validProperties();
        var topology = validator.validate(properties, yaml(false));
        var source = org.mockito.Mockito.mock(javax.sql.DataSource.class);
        var connection = org.mockito.Mockito.mock(java.sql.Connection.class);
        var metadata = org.mockito.Mockito.mock(java.sql.DatabaseMetaData.class);
        var statement = org.mockito.Mockito.mock(java.sql.Statement.class);
        var role = org.mockito.Mockito.mock(java.sql.ResultSet.class);
        var history = org.mockito.Mockito.mock(java.sql.PreparedStatement.class);
        var historyRows = org.mockito.Mockito.mock(java.sql.ResultSet.class);
        var columns = org.mockito.Mockito.mock(java.sql.ResultSet.class);
        org.mockito.Mockito.when(source.getConnection()).thenReturn(connection);
        org.mockito.Mockito.when(connection.getMetaData()).thenReturn(metadata);
        org.mockito.Mockito.when(connection.getSchema()).thenReturn("public");
        org.mockito.Mockito.when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        org.mockito.Mockito.when(connection.createStatement()).thenReturn(statement);
        org.mockito.Mockito.when(statement.executeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(role);
        org.mockito.Mockito.when(role.next()).thenReturn(true);
        org.mockito.Mockito.when(role.getBoolean(1)).thenReturn(false);
        org.mockito.Mockito.when(role.getBoolean(2)).thenReturn(true);
        org.mockito.Mockito.when(connection.prepareStatement(org.mockito.ArgumentMatchers.anyString())).thenReturn(history);
        org.mockito.Mockito.when(history.executeQuery()).thenReturn(historyRows);
        org.mockito.Mockito.when(historyRows.next()).thenReturn(true);
        org.mockito.Mockito.when(historyRows.getString(1)).thenReturn("0".repeat(64));
        org.mockito.Mockito.when(historyRows.getString(2)).thenReturn(topology.fingerprint());
        org.mockito.Mockito.when(metadata.getColumns(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(columns);
        org.mockito.Mockito.when(columns.next()).thenReturn(true);
        org.mockito.Mockito.when(columns.getInt("DATA_TYPE")).thenReturn(java.sql.Types.BIGINT);
        org.mockito.Mockito.when(columns.getInt("NULLABLE")).thenReturn(java.sql.DatabaseMetaData.columnNoNulls);
        var manifest = new top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO("light-open", List.of(
                new top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO.ScriptBO("20260913_001", "db/test.sql", "0".repeat(64))));
        var targets = properties.ddl().targets().stream().map(target -> new EgonColaDdlTargetBO(target.dataSourceName(), target.schema(), target.role(), source, manifest, topology.fingerprint())).toList();
        assertThatThrownBy(() -> validator.verifyReadiness(topology, properties,
                java.util.Map.of("master_data", source, "shard_0", source, "shard_1", source), targets, java.time.Duration.ofSeconds(1)))
                .hasRootCauseMessage("PHYSICAL_ROLE_MISMATCH");
    }

    @Test
    void validatesTypedSingleAndLegacyRulesWithoutChangingLegacyAddresses() {
        var result = validator.validate(validProperties(), yaml(false));
        assertThat(result.profiles()).hasSize(8);
        assertThat(result.profiles().get("light_users").kind()).isEqualTo(EgonColaRoutingProfileBO.TableKindEnum.SINGLE);
        assertThat(result.profiles().get("light_school_classes").kind()).isEqualTo(EgonColaRoutingProfileBO.TableKindEnum.TENANT_LEGACY);
        assertThat(result.legacy().route(41L)).isEqualTo(new ShardingNodeMap.PhysicalNode("shard_0", 1));
        assertThat(result.fingerprint()).matches("[a-f0-9]{64}");
    }

    @Test
    void validatesReadwriteGroupsAndPrimaryTransactionalReads() {
        assertThat(validator.validate(validReadwriteProperties(), yaml(true)).profiles()).hasSize(8);
        String changed = new String(yaml(true), StandardCharsets.UTF_8).replace("transactionalReadQueryStrategy: PRIMARY", "transactionalReadQueryStrategy: FIXED");
        assertThatThrownBy(() -> validator.validate(validReadwriteProperties(), changed.getBytes(StandardCharsets.UTF_8)))
                .hasMessageContaining("READWRITE_PRIMARY_REPLICA_POLICY_INVALID");
    }

    @Test
    void rejectsMissingOrReplicaDdlTargetsAndDuplicatePrimaries() {
        var good = validReadwriteProperties();
        var missing = new ShardingDataSourceProperties(good.config(), good.routing(), good.physicalDataSources(),
                new ShardingDataSourceProperties.ShardingDdlProperties(good.ddl().targets().subList(0, 2)));
        assertThatThrownBy(() -> validator.validate(missing, yaml(true))).hasMessageContaining("DDL_PRIMARY_COVERAGE_REQUIRED");
        var targets = new ArrayList<>(good.ddl().targets());
        targets.set(0, target("master_data_replica_0", EgonColaDdlTargetBO.RoleEnum.MASTER_DATA));
        assertThatThrownBy(() -> validator.validate(new ShardingDataSourceProperties(good.config(), good.routing(), good.physicalDataSources(),
                new ShardingDataSourceProperties.ShardingDdlProperties(targets)), yaml(true))).hasMessageContaining("DDL_TARGET_MUST_BE_UNIQUE_PRIMARY");
        var sources = new ArrayList<>(good.physicalDataSources());
        sources.add(physical("second_primary", "shard_0", ShardingDataSourceProperties.DataSourceRole.PRIMARY));
        assertThatThrownBy(() -> validator.validate(new ShardingDataSourceProperties(good.config(), good.routing(), sources, good.ddl()), yaml(true)))
                .hasMessageContaining("EXACTLY_ONE_PRIMARY_REQUIRED");
    }

    @Test
    void rejectsWildcardSingleNodesIncorrectSchemaAndUnmanagedGroups() {
        String valid = new String(yaml(false), StandardCharsets.UTF_8);
        for (String invalid : List.of(valid.replace("master_data.public.light_users", "master_data.*.*"),
                valid.replace("master_data.public.light_users", "master_data.other.light_users"),
                valid.replace("master_data.public.light_users", "unknown.public.light_users"))) {
            assertThatThrownBy(() -> validator.validate(validProperties(), invalid.getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsChangedLegacyMapOrAnAlgorithmOutsideTheSupportedContract() {
        String valid = new String(yaml(false), StandardCharsets.UTF_8);
        assertThatThrownBy(() -> validator.validate(validProperties(), valid.replaceFirst("node-count: 4", "node-count: 8").getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(validProperties(), valid.replace("LongTenantShardingAlgorithm", "UnknownAlgorithm").getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(validProperties(), valid.replace("defaultType: LOCAL", "defaultType: XA").getBytes(StandardCharsets.UTF_8)))
                .hasMessageContaining("LOCAL_TRANSACTION_REQUIRED");
    }

    @Test
    void physicalCollectionOrderDoesNotChangeTheFingerprint() {
        var good = validProperties();
        var sources = new ArrayList<>(good.physicalDataSources());
        Collections.reverse(sources);
        var reordered = new ShardingDataSourceProperties(good.config(), good.routing(), sources, good.ddl());
        assertThat(validator.validate(reordered, yaml(false)).fingerprint()).isEqualTo(validator.validate(good, yaml(false)).fingerprint());
    }

    static ShardingTopologyValidator.TopologyBO validated(ShardingDataSourceProperties properties, byte[] yaml) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            return new ShardingTopologyValidator(new ValidationUtils(factory.getValidator())).validate(properties, yaml);
        }
    }

    static ShardingDataSourceProperties validProperties() { return properties(false); }
    static ShardingDataSourceProperties validReadwriteProperties() { return properties(true); }

    private static ShardingDataSourceProperties properties(boolean readwrite) {
        List<ShardingDataSourceProperties.PhysicalDataSourceProperties> sources = new ArrayList<>();
        List<ShardingDataSourceProperties.DdlTargetProperties> targets = new ArrayList<>();
        for (String group : List.of("master_data", "shard_0", "shard_1")) {
            String primary = readwrite ? group + "_primary" : group;
            sources.add(physical(primary, group, ShardingDataSourceProperties.DataSourceRole.PRIMARY));
            if (readwrite) { sources.add(physical(group + "_replica_0", group, ShardingDataSourceProperties.DataSourceRole.REPLICA)); }
            targets.add(target(primary, group.equals("master_data") ? EgonColaDdlTargetBO.RoleEnum.MASTER_DATA : EgonColaDdlTargetBO.RoleEnum.SHARD));
        }
        return new ShardingDataSourceProperties("classpath:sharding/shardingsphere-sharding" + (readwrite ? "-readwrite" : "") + ".yml",
                new ShardingDataSourceProperties.ShardingRoutingProperties(4, "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1"),
                sources, new ShardingDataSourceProperties.ShardingDdlProperties(targets));
    }

    private static ShardingDataSourceProperties.PhysicalDataSourceProperties physical(String name, String group, ShardingDataSourceProperties.DataSourceRole role) {
        return new ShardingDataSourceProperties.PhysicalDataSourceProperties(name, group, role, "org.postgresql.Driver", "jdbc:postgresql://localhost/test", "test", "secret");
    }

    private static ShardingDataSourceProperties.DdlTargetProperties target(String name, EgonColaDdlTargetBO.RoleEnum role) {
        return new ShardingDataSourceProperties.DdlTargetProperties(name, "public", role, "classpath:db/egon-mp/repository-manifest.json");
    }

    static byte[] yaml(boolean readwrite) {
        return new ShardingYamlLoader(new DefaultResourceLoader(), new MockEnvironment()
                .withProperty("app.sharding.routing.node-count", "4")
                .withProperty("app.sharding.routing.node-map", "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1"))
                .load("classpath:sharding/shardingsphere-sharding" + (readwrite ? "-readwrite" : "") + ".yml");
    }
}
