package top.egon.cola.archetype.source.serviceopen.infrastructure.config.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.yaml.YamlJDBCConfiguration;
import org.apache.shardingsphere.infra.algorithm.core.yaml.YamlAlgorithmConfiguration;
import org.apache.shardingsphere.infra.util.yaml.YamlEngine;
import org.apache.shardingsphere.sharding.yaml.config.YamlShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.yaml.config.rule.YamlTableRuleConfiguration;
import org.apache.shardingsphere.single.yaml.config.YamlSingleRuleConfiguration;
import org.apache.shardingsphere.broadcast.yaml.config.YamlBroadcastRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.yaml.config.YamlReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.transaction.yaml.config.YamlTransactionRuleConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Validates one typed SS policy, then verifies physical metadata before logical datasource creation. */
@Slf4j
@RequiredArgsConstructor
public final class ShardingTopologyValidator {
    private static final long SEED = 0x9e3779b97f4a7c15L;
    private static final Pattern RANGE = Pattern.compile("\\$->\\{(\\d+)\\.\\.(\\d+)}");
    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    public TopologyBO validate(ShardingDataSourceProperties properties, byte[] yaml) {
        validation.validate(properties);
        if (yaml == null || yaml.length == 0) { throw new IllegalArgumentException("SHARDING_YAML_REQUIRED"); }
        YamlJDBCConfiguration configuration = YamlEngine.unmarshal(new String(yaml, StandardCharsets.UTF_8), YamlJDBCConfiguration.class);
        if (!configuration.getDataSources().isEmpty()) { throw new IllegalArgumentException("PHYSICAL_DATASOURCES_MUST_HAVE_ONE_OWNER"); }
        if (configuration.getTransaction() == null) { configuration.setTransaction(new YamlTransactionRuleConfiguration()); }
        if (!"LOCAL".equals(configuration.getTransaction().getDefaultType())) { throw new IllegalArgumentException("LOCAL_TRANSACTION_REQUIRED"); }
        ShardingNodeMap legacy = ShardingNodeMap.parse(routing(properties.routing()));
        Map<String, List<ShardingDataSourceProperties.PhysicalDataSourceProperties>> groups = properties.physicalDataSources().stream()
                .collect(Collectors.groupingBy(ShardingDataSourceProperties.PhysicalDataSourceProperties::logicalName, TreeMap::new, Collectors.toList()));
        Map<String, ShardingDataSourceProperties.PhysicalDataSourceProperties> sources = new LinkedHashMap<>();
        properties.physicalDataSources().forEach(source -> {
            if (sources.put(source.name(), source) != null) { throw new IllegalArgumentException("DUPLICATE_PHYSICAL_DATASOURCE"); }
        });
        Set<String> primaries = new HashSet<>();
        groups.forEach((name, members) -> {
            var primary = members.stream().filter(source -> source.role() == ShardingDataSourceProperties.DataSourceRole.PRIMARY).toList();
            if (primary.size() != 1) { throw new IllegalArgumentException("EXACTLY_ONE_PRIMARY_REQUIRED"); }
            primaries.add(primary.getFirst().name());
        });
        Map<String, String> schemas = new TreeMap<>();
        Set<String> targets = new HashSet<>();
        for (var target : properties.ddl().targets()) {
            var source = sources.get(target.dataSourceName());
            if (source == null || source.role() != ShardingDataSourceProperties.DataSourceRole.PRIMARY || !targets.add(source.name())) {
                throw new IllegalArgumentException("DDL_TARGET_MUST_BE_UNIQUE_PRIMARY");
            }
            if (!target.manifest().startsWith("classpath:") || target.manifest().contains("..")) { throw new IllegalArgumentException("CLASSPATH_MANIFEST_REQUIRED"); }
            schemas.put(source.logicalName(), target.schema());
        }
        if (!targets.equals(primaries)) { throw new IllegalArgumentException("DDL_PRIMARY_COVERAGE_REQUIRED"); }
        Map<String, EgonColaRoutingProfileBO> profiles = new TreeMap<>();
        List<YamlShardingRuleConfiguration> shardings = new ArrayList<>();
        YamlReadwriteSplittingRuleConfiguration readwrite = null;
        Set<String> broadcast = new LinkedHashSet<>();
        for (var rule : configuration.getRules()) {
            if (rule instanceof YamlSingleRuleConfiguration single) {
                if (single.getDefaultDataSource() != null && !single.getDefaultDataSource().isBlank()) { throw new IllegalArgumentException("SINGLE_DEFAULT_FORBIDDEN"); }
                for (String table : single.getTables()) {
                    String[] parts = table.split("\\.");
                    if (parts.length != 3 || table.contains("*")) { throw new IllegalArgumentException("SINGLE_GROUP_SCHEMA_TABLE_REQUIRED"); }
                    EgonColaPhysicalTargetBO node = new EgonColaPhysicalTargetBO(parts[0], parts[1], parts[2]);
                    validateNode(node, schemas);
                    put(profiles, simple(parts[2], EgonColaRoutingProfileBO.TableKindEnum.SINGLE, List.of(node)));
                }
            } else if (rule instanceof YamlBroadcastRuleConfiguration copies) {
                for (String table : copies.getTables()) {
                    List<EgonColaPhysicalTargetBO> nodes = schemas.entrySet().stream()
                            .map(entry -> new EgonColaPhysicalTargetBO(entry.getKey(), entry.getValue(), table)).toList();
                    put(profiles, simple(table, EgonColaRoutingProfileBO.TableKindEnum.BROADCAST_READ_ONLY, nodes));
                    broadcast.add(table);
                }
            } else if (rule instanceof YamlShardingRuleConfiguration sharding) {
                // YAML retains numeric scalars, while the ShardingSphere SPI reads string properties.
                sharding.getShardingAlgorithms().values().forEach(algorithm -> {
                    Properties normalized = new Properties();
                    algorithm.getProps().forEach((key, value) -> normalized.setProperty(key.toString(), value.toString()));
                    algorithm.setProps(normalized);
                });
                shardings.add(sharding);
            } else if (rule instanceof YamlReadwriteSplittingRuleConfiguration splitting) {
                if (readwrite != null) { throw new IllegalArgumentException("DUPLICATE_READWRITE_RULE"); }
                readwrite = splitting;
            } else { throw new IllegalArgumentException("UNSUPPORTED_SHARDING_RULE"); }
        }
        if (shardings.size() > 1) { throw new IllegalArgumentException("DUPLICATE_SHARDING_RULE"); }
        String readwriteFingerprint = validateReadwrite(groups, readwrite);
        for (YamlShardingRuleConfiguration sharding : shardings) { addSharding(sharding, legacy, schemas, profiles); }
        if (profiles.isEmpty()) { throw new IllegalArgumentException("LOGICAL_TABLES_REQUIRED"); }
        Set<String> actual = new HashSet<>();
        for (var profile : profiles.values()) for (var nodes : profile.actualNodes().values()) for (var node : nodes) {
            if (!actual.add(node.group() + '.' + node.schema() + '.' + node.table())) { throw new IllegalArgumentException("PHYSICAL_TABLE_RULE_OVERLAP"); }
        }
        String physical = sources.values().stream().map(source -> source.name() + ':' + source.logicalName() + ':' + source.role()).sorted().collect(Collectors.joining("|"));
        String fingerprint = digest("ss-policy-v1|" + profiles + '|' + legacy + '|' + physical + '|' + readwriteFingerprint);
        return new TopologyBO(YamlEngine.marshal(configuration).getBytes(StandardCharsets.UTF_8), profiles, legacy, schemas, broadcast, fingerprint);
    }

    private void addSharding(YamlShardingRuleConfiguration rule, ShardingNodeMap legacy, Map<String, String> schemas,
                             Map<String, EgonColaRoutingProfileBO> profiles) {
        if (!rule.getAutoTables().isEmpty() || rule.getDefaultDatabaseStrategy() != null || rule.getDefaultTableStrategy() != null
                || rule.getDefaultKeyGenerateStrategy() != null || !rule.getKeyGenerators().isEmpty()) {
            throw new IllegalArgumentException("EXPLICIT_ROUTING_AND_APPLICATION_IDS_REQUIRED");
        }
        Map<String, String> bindings = new HashMap<>();
        for (String group : rule.getBindingTables()) {
            List<String> tables = List.of(group.split(",")).stream().map(String::trim).sorted().toList();
            String key = "binding_" + digest(String.join(",", tables)).substring(0, 16);
            for (String table : tables) {
                if (bindings.put(table, key) != null || !rule.getTables().containsKey(table)) { throw new IllegalArgumentException("INVALID_BINDING_TABLE_GROUP"); }
            }
        }
        Map<String, YamlAlgorithmConfiguration> algorithms = new LinkedHashMap<>();
        Set<String> referenced = new HashSet<>();
        for (var entry : rule.getTables().entrySet()) {
            String table = entry.getKey();
            YamlTableRuleConfiguration configured = entry.getValue();
            if (configured.getKeyGenerateStrategy() != null || configured.getDatabaseStrategy() == null
                    || configured.getDatabaseStrategy().getStandard() == null || configured.getTableStrategy() == null) {
                throw new IllegalArgumentException("EXPLICIT_TENANT_STRATEGY_REQUIRED");
            }
            var database = configured.getDatabaseStrategy().getStandard();
            if (!"tenant_id".equals(database.getShardingColumn())) { throw new IllegalArgumentException("TENANT_DATABASE_KEY_REQUIRED"); }
            var db = algorithm(rule, database.getShardingAlgorithmName(), referenced);
            List<EgonColaPhysicalTargetBO> nodes = expand(configured.getActualDataNodes(), schemas);
            configured.setActualDataNodes(nodes.stream().map(node -> node.group() + '.' + node.schema() + '.' + node.table()).collect(Collectors.joining(",")));
            if (configured.getTableStrategy().getStandard() != null) {
                var strategy = configured.getTableStrategy().getStandard();
                var tableAlgorithm = algorithm(rule, strategy.getShardingAlgorithmName(), referenced);
                if (!"tenant_id".equals(strategy.getShardingColumn()) || !legacyAlgorithm(db, "database", legacy)
                        || !legacyAlgorithm(tableAlgorithm, "table", legacy)) { throw new IllegalArgumentException("LEGACY_ROUTING_POLICY_MISMATCH"); }
                Set<EgonColaPhysicalTargetBO> expected = legacy.nodes().values().stream().map(node ->
                        new EgonColaPhysicalTargetBO(node.database(), schemas.get(node.database()), table + '_' + node.tableSuffix())).collect(Collectors.toSet());
                if (!expected.equals(new HashSet<>(nodes))) { throw new IllegalArgumentException("ACTUAL_NODES_MISMATCH"); }
                put(profiles, new EgonColaRoutingProfileBO(table, EgonColaRoutingProfileBO.TableKindEnum.TENANT_LEGACY,
                        "legacy-v1", 1, 1, Map.of(), null, null, SEED,
                        Map.of(new EgonColaRoutingProfileBO.PartitionKeyBO(0, 0), nodes), 1, bindings.get(table)));
                algorithms.put(database.getShardingAlgorithmName(), db);
                algorithms.put(strategy.getShardingAlgorithmName(), tableAlgorithm);
            } else if (configured.getTableStrategy().getComplex() != null) {
                var strategy = configured.getTableStrategy().getComplex();
                var tableAlgorithm = algorithm(rule, strategy.getShardingAlgorithmName(), referenced);
                if (!TenantDatabaseShardingAlgorithm.class.getName().equals(db.getProps().getProperty("algorithmClassName"))
                        || !TenantBusinessTableShardingAlgorithm.class.getName().equals(tableAlgorithm.getProps().getProperty("algorithmClassName"))
                        || !"STANDARD".equals(db.getProps().getProperty("strategy")) || !"COMPLEX".equals(tableAlgorithm.getProps().getProperty("strategy"))) {
                    throw new IllegalArgumentException("TWO_LEVEL_ALGORITHM_REQUIRED");
                }
                for (String key : List.of("algorithm-version", "tenant-slot-count", "tenant-slot-map")) {
                    if (!Objects.equals(db.getProps().getProperty(key), tableAlgorithm.getProps().getProperty(key))) {
                        throw new IllegalArgumentException("TWO_LEVEL_POLICY_MISMATCH");
                    }
                }
                String secondary = tableAlgorithm.getProps().getProperty("secondary-column");
                Set<String> columns = List.of(strategy.getShardingColumns().split(",")).stream().map(String::trim).collect(Collectors.toSet());
                if (!columns.equals(Set.of("tenant_id", secondary))) { throw new IllegalArgumentException("SECONDARY_COLUMN_MISMATCH"); }
                Properties complete = new Properties();
                complete.putAll(tableAlgorithm.getProps());
                complete.setProperty("logical-table", table);
                complete.setProperty("actual-data-nodes", nodes.stream().map(node -> node.group() + '.' + node.schema() + '.' + node.table()).collect(Collectors.joining(",")));
                if (bindings.containsKey(table)) { complete.setProperty("binding-group", bindings.get(table)); }
                EgonColaRoutingProfileBO profile = validation.validate(ShardingWriteTargetResolver.profile(complete));
                put(profiles, profile);
                // SS constructs unmanaged SPI objects. Enrich per-table algorithm copies from this same typed rule.
                String dbName = database.getShardingAlgorithmName() + "__" + table;
                String tableName = strategy.getShardingAlgorithmName() + "__" + table;
                algorithms.put(dbName, copyAlgorithm(complete, TenantDatabaseShardingAlgorithm.class.getName(), "STANDARD"));
                algorithms.put(tableName, copyAlgorithm(complete, TenantBusinessTableShardingAlgorithm.class.getName(), "COMPLEX"));
                database.setShardingAlgorithmName(dbName);
                strategy.setShardingAlgorithmName(tableName);
            } else { throw new IllegalArgumentException("UNSUPPORTED_TABLE_STRATEGY"); }
        }
        if (!referenced.equals(rule.getShardingAlgorithms().keySet())) { throw new IllegalArgumentException("UNUSED_SHARDING_ALGORITHM"); }
        rule.setShardingAlgorithms(algorithms);
        for (String binding : new HashSet<>(bindings.values())) {
            List<EgonColaRoutingProfileBO> members = profiles.values().stream().filter(profile -> binding.equals(profile.bindingGroup())).toList();
            EgonColaRoutingProfileBO first = members.getFirst();
            if (members.stream().anyMatch(profile -> profile.kind() != first.kind() || profile.tenantSlotCount() != first.tenantSlotCount()
                    || profile.secondaryBucketCount() != first.secondaryBucketCount() || !profile.tenantSlotMap().equals(first.tenantSlotMap())
                    || !Objects.equals(profile.rootKeyName(), first.rootKeyName()) || !profile.algorithmVersion().equals(first.algorithmVersion())
                    || profile.secondarySeed() != first.secondarySeed())) { throw new IllegalArgumentException("BINDING_POLICY_MISMATCH"); }
        }
    }

    private static YamlAlgorithmConfiguration algorithm(YamlShardingRuleConfiguration rule, String name, Set<String> referenced) {
        YamlAlgorithmConfiguration value = rule.getShardingAlgorithms().get(name);
        if (value == null || !"CLASS_BASED".equals(value.getType())) { throw new IllegalArgumentException("CLASS_BASED_ALGORITHM_REQUIRED"); }
        referenced.add(name);
        return value;
    }

    private static YamlAlgorithmConfiguration copyAlgorithm(Properties source, String type, String strategy) {
        YamlAlgorithmConfiguration result = new YamlAlgorithmConfiguration();
        result.setType("CLASS_BASED");
        Properties props = new Properties();
        props.putAll(source);
        props.setProperty("algorithmClassName", type);
        props.setProperty("strategy", strategy);
        result.setProps(props);
        return result;
    }

    private static boolean legacyAlgorithm(YamlAlgorithmConfiguration algorithm, String target, ShardingNodeMap expected) {
        return LongTenantShardingAlgorithm.class.getName().equals(algorithm.getProps().getProperty("algorithmClassName"))
                && "STANDARD".equals(algorithm.getProps().getProperty("strategy"))
                && target.equals(algorithm.getProps().getProperty("target")) && expected.equals(ShardingNodeMap.parse(algorithm.getProps()));
    }

    private static String validateReadwrite(Map<String, List<ShardingDataSourceProperties.PhysicalDataSourceProperties>> groups,
                                            YamlReadwriteSplittingRuleConfiguration readwrite) {
        if (readwrite == null) {
            groups.forEach((group, members) -> {
                if (members.size() != 1 || members.getFirst().role() != ShardingDataSourceProperties.DataSourceRole.PRIMARY
                        || !group.equals(members.getFirst().name())) { throw new IllegalArgumentException("PRIMARY_ONLY_GROUP_REQUIRED"); }
            });
            return "primary-only";
        }
        if (!groups.keySet().equals(readwrite.getDataSourceGroups().keySet())) { throw new IllegalArgumentException("READWRITE_GROUPS_MISMATCH"); }
        List<String> fingerprint = new ArrayList<>();
        groups.forEach((name, sources) -> {
            var rule = readwrite.getDataSourceGroups().get(name);
            String writer = sources.stream().filter(source -> source.role() == ShardingDataSourceProperties.DataSourceRole.PRIMARY).findFirst().orElseThrow().name();
            Set<String> readers = sources.stream().filter(source -> source.role() == ShardingDataSourceProperties.DataSourceRole.REPLICA)
                    .map(ShardingDataSourceProperties.PhysicalDataSourceProperties::name).collect(Collectors.toSet());
            var balancer = readwrite.getLoadBalancers().get(rule.getLoadBalancerName());
            if (!writer.equals(rule.getWriteDataSourceName()) || readers.isEmpty() || !readers.equals(new HashSet<>(rule.getReadDataSourceNames()))
                    || !"PRIMARY".equals(rule.getTransactionalReadQueryStrategy().toString()) || balancer == null || !"ROUND_ROBIN".equals(balancer.getType())) {
                throw new IllegalArgumentException("READWRITE_PRIMARY_REPLICA_POLICY_INVALID");
            }
            fingerprint.add(name + '>' + writer + '>' + readers.stream().sorted().toList());
        });
        return fingerprint.stream().sorted().collect(Collectors.joining("|"));
    }

    private static List<EgonColaPhysicalTargetBO> expand(String value, Map<String, String> schemas) {
        if (value == null || value.isBlank()) { throw new IllegalArgumentException("ACTUAL_NODES_REQUIRED"); }
        List<String> expanded = new ArrayList<>();
        for (String item : value.split(",")) { expandRange(item.trim(), expanded); }
        List<EgonColaPhysicalTargetBO> result = new ArrayList<>();
        for (String item : expanded) {
            String[] parts = item.split("\\.");
            EgonColaPhysicalTargetBO node = parts.length == 2 ? new EgonColaPhysicalTargetBO(parts[0], schemas.get(parts[0]), parts[1])
                    : parts.length == 3 ? new EgonColaPhysicalTargetBO(parts[0], parts[1], parts[2]) : null;
            if (node == null) { throw new IllegalArgumentException("INVALID_ACTUAL_NODE"); }
            validateNode(node, schemas);
            result.add(node);
        }
        if (new HashSet<>(result).size() != result.size()) { throw new IllegalArgumentException("DUPLICATE_ACTUAL_NODE"); }
        return result.stream().sorted(Comparator.comparing(EgonColaPhysicalTargetBO::group).thenComparing(EgonColaPhysicalTargetBO::schema)
                .thenComparing(EgonColaPhysicalTargetBO::table)).toList();
    }

    private static void expandRange(String value, List<String> result) {
        var range = RANGE.matcher(value);
        if (!range.find()) {
            if (value.contains("$") || value.contains("*")) { throw new IllegalArgumentException("UNSUPPORTED_INLINE_EXPRESSION"); }
            result.add(value);
            return;
        }
        int from = Integer.parseInt(range.group(1));
        int to = Integer.parseInt(range.group(2));
        if (from > to || to - from > 4096 || result.size() > 4096) { throw new IllegalArgumentException("ACTUAL_NODE_LIMIT_EXCEEDED"); }
        for (long index = from; index <= to; index++) { expandRange(value.substring(0, range.start()) + index + value.substring(range.end()), result); }
    }

    private static EgonColaRoutingProfileBO simple(String table, EgonColaRoutingProfileBO.TableKindEnum kind, List<EgonColaPhysicalTargetBO> nodes) {
        return new EgonColaRoutingProfileBO(table, kind, "static-v1", 1, 1, Map.of(), null, null, SEED,
                Map.of(new EgonColaRoutingProfileBO.PartitionKeyBO(0, 0), nodes), 1, null);
    }

    private static void put(Map<String, EgonColaRoutingProfileBO> profiles, EgonColaRoutingProfileBO profile) {
        if (profiles.put(profile.logicalTable(), profile) != null) { throw new IllegalArgumentException("LOGICAL_TABLE_RULE_OVERLAP"); }
    }

    private static void validateNode(EgonColaPhysicalTargetBO node, Map<String, String> schemas) {
        if (!Objects.equals(schemas.get(node.group()), node.schema())) { throw new IllegalArgumentException("ACTUAL_NODE_SCHEMA_OR_GROUP_MISMATCH"); }
    }

    static Properties routing(ShardingDataSourceProperties.ShardingRoutingProperties routing) {
        Properties values = new Properties();
        values.setProperty("node-count", Integer.toString(routing.nodeCount()));
        values.setProperty("node-map", routing.nodeMap());
        return values;
    }

    public void verifyReadiness(TopologyBO topology, ShardingDataSourceProperties properties, Map<String, DataSource> physical,
                                List<EgonColaDdlTargetBO> targets, Duration timeout) {
        Map<String, String> broadcastHashes = new HashMap<>();
        for (var source : properties.physicalDataSources()) {
            var target = targets.stream().filter(value -> properties.physicalDataSources().stream()
                    .anyMatch(candidate -> candidate.name().equals(value.alias()) && candidate.logicalName().equals(source.logicalName()))).findFirst().orElseThrow();
            Instant deadline = Instant.now().plus(timeout);
            while (true) {
                try {
                    verifySource(topology, source, physical.get(source.name()), target, broadcastHashes);
                    break;
                } catch (SQLException | IllegalStateException failure) {
                    if (source.role() == ShardingDataSourceProperties.DataSourceRole.PRIMARY || !Instant.now().isBefore(deadline)) {
                        throw new IllegalStateException("SHARDING_TOPOLOGY_NOT_READY: " + source.name(), failure);
                    }
                    try { Thread.sleep(100); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("TOPOLOGY_CHECK_INTERRUPTED", interrupted); }
                }
            }
        }
    }

    private static void verifySource(TopologyBO topology, ShardingDataSourceProperties.PhysicalDataSourceProperties source,
                                      DataSource dataSource, EgonColaDdlTargetBO target, Map<String, String> broadcastHashes) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()) || !target.schema().equals(connection.getSchema())) {
                throw new IllegalStateException("POSTGRESQL_SCHEMA_REQUIRED");
            }
            try (var statement = connection.createStatement(); var row = statement.executeQuery("SELECT pg_is_in_recovery(), current_setting('transaction_read_only')::boolean")) {
                if (!row.next() || row.getBoolean(1) != (source.role() == ShardingDataSourceProperties.DataSourceRole.REPLICA)
                        || row.getBoolean(2) != (source.role() == ShardingDataSourceProperties.DataSourceRole.REPLICA)) { throw new IllegalStateException("PHYSICAL_ROLE_MISMATCH"); }
            }
            var script = target.manifest().scripts().getLast();
            try (var statement = connection.prepareStatement("SELECT checksum,route_fingerprint FROM \"" + target.schema()
                    + "\".ddl_history WHERE type='SQL' AND version=? AND tenant_id=0")) {
                statement.setString(1, script.version());
                try (var rows = statement.executeQuery()) {
                    if (!rows.next() || !script.sha256().equals(rows.getString(1)) || !topology.fingerprint().equals(rows.getString(2))) {
                        throw new IllegalStateException("DDL_REPLICA_HISTORY_NOT_READY");
                    }
                }
            }
            Set<String> tables = topology.profiles().values().stream().flatMap(profile -> profile.actualNodes().values().stream())
                    .flatMap(Collection::stream).filter(node -> node.group().equals(source.logicalName())).map(EgonColaPhysicalTargetBO::table).collect(Collectors.toSet());
            for (String table : tables) {
                try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), target.schema(), table, "tenant_id")) {
                    if (!columns.next() || columns.getInt("DATA_TYPE") != Types.BIGINT || columns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls) {
                        throw new IllegalStateException("TENANT_COLUMN_METADATA_NOT_READY");
                    }
                }
                if (topology.broadcastTables().contains(table)) {
                    String hash = broadcastDigest(connection, target.schema(), table);
                    String previous = broadcastHashes.putIfAbsent(table, hash);
                    if (previous != null && !previous.equals(hash)) { throw new IllegalStateException("BROADCAST_CONTENT_MISMATCH"); }
                }
            }
        }
    }

    private static String broadcastDigest(Connection connection, String schema, String table) throws SQLException {
        StringBuilder canonical = new StringBuilder();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT * FROM \"" + schema + "\".\"" + table + "\" ORDER BY tenant_id,id")) {
            var metadata = rows.getMetaData();
            while (rows.next()) {
                for (int column = 1; column <= metadata.getColumnCount(); column++) {
                    String name = metadata.getColumnName(column);
                    if (Set.of("create_user_id", "create_time", "update_user_id", "update_time").contains(name)) { continue; }
                    String value = "deleted_at".equals(name) ? Boolean.toString(rows.getObject(column) == null) : rows.getString(column);
                    canonical.append(name.length()).append(':').append(name).append('=').append(value == null ? -1 : value.length()).append(':');
                    if (value != null) { canonical.append(value); }
                    canonical.append(';');
                }
                canonical.append('\n');
            }
        }
        return digest(canonical.toString());
    }

    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("SHA256_REQUIRED", failure); }
    }

    public record TopologyBO(byte[] yaml, Map<String, EgonColaRoutingProfileBO> profiles, ShardingNodeMap legacy,
                             Map<String, String> schemas, Set<String> broadcastTables, String fingerprint) {
        public TopologyBO {
            yaml = yaml.clone();
            profiles = java.util.Collections.unmodifiableMap(new TreeMap<>(profiles));
            schemas = Map.copyOf(schemas);
            broadcastTables = Set.copyOf(broadcastTables);
        }
        @Override public byte[] yaml() { return yaml.clone(); }
    }
}
