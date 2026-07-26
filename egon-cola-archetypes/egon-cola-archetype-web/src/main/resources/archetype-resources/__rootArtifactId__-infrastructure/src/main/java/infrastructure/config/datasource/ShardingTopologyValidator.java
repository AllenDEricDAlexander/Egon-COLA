package ${package}.infrastructure.config.datasource;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates that physical groups, stable routing and Flyway targets describe one topology.
 */
public final class ShardingTopologyValidator {

    private static final Pattern INLINE_RANGE =
            Pattern.compile("^(.*)\\$->\\{(\\d+)\\.\\.(\\d+)}$");

    public void validate(ShardingDataSourceProperties properties, byte[] yaml) {
        if (properties == null || properties.routing() == null) {
            throw new IllegalArgumentException("sharding routing properties must not be null");
        }
        ShardingNodeMap nodeMap = parseNodeMap(properties.routing());
        Set<String> expectedLogicalNames = new LinkedHashSet<>();
        expectedLogicalNames.add("master_data");
        nodeMap.nodes().values().stream()
                .map(ShardingNodeMap.PhysicalNode::database)
                .forEach(expectedLogicalNames::add);

        Map<String, List<ShardingDataSourceProperties.PhysicalDataSourceProperties>>
                sourcesByLogicalName = properties.physicalDataSources().stream()
                        .collect(Collectors.groupingBy(
                                ShardingDataSourceProperties.PhysicalDataSourceProperties
                                        ::logicalName,
                                LinkedHashMap::new,
                                Collectors.toList()));
        if (!sourcesByLogicalName.keySet().equals(expectedLogicalNames)) {
            Set<String> difference = new LinkedHashSet<>(expectedLogicalNames);
            difference.removeAll(sourcesByLogicalName.keySet());
            if (difference.isEmpty()) {
                difference.addAll(sourcesByLogicalName.keySet());
                difference.removeAll(expectedLogicalNames);
            }
            throw new IllegalArgumentException(
                    "physical logical groups do not match routing topology: " + difference);
        }

        Set<String> primaryNames = validateRoles(sourcesByLogicalName);
        validateFlywayTargets(properties.flyway(), properties.physicalDataSources(), primaryNames);
        validateRuleRouting(properties.routing(), nodeMap, sourcesByLogicalName, yaml);
    }

    private static ShardingNodeMap parseNodeMap(
            ShardingDataSourceProperties.ShardingRoutingProperties routing) {
        Properties values = new Properties();
        values.setProperty("node-count", Integer.toString(routing.nodeCount()));
        if (routing.nodeMap() != null) {
            values.setProperty("node-map", routing.nodeMap());
        }
        return ShardingNodeMap.parse(values);
    }

    private static Set<String> validateRoles(
            Map<String, List<ShardingDataSourceProperties.PhysicalDataSourceProperties>>
                    sourcesByLogicalName) {
        Set<String> physicalNames = new HashSet<>();
        Set<String> primaryNames = new LinkedHashSet<>();
        sourcesByLogicalName.forEach((logicalName, sources) -> {
            long primaryCount = sources.stream()
                    .filter(source -> source.role()
                            == ShardingDataSourceProperties.DataSourceRole.PRIMARY)
                    .count();
            if (primaryCount != 1) {
                throw new IllegalArgumentException(
                        "logical group " + logicalName + " must have exactly one primary");
            }
            for (ShardingDataSourceProperties.PhysicalDataSourceProperties source : sources) {
                if (!physicalNames.add(source.name())) {
                    throw new IllegalArgumentException(
                            "duplicate physical data source name: " + source.name());
                }
                if (source.role() == ShardingDataSourceProperties.DataSourceRole.PRIMARY) {
                    primaryNames.add(source.name());
                }
            }
        });
        return primaryNames;
    }

    private static void validateFlywayTargets(
            ShardingDataSourceProperties.ShardingFlywayProperties flyway,
            List<ShardingDataSourceProperties.PhysicalDataSourceProperties> dataSources,
            Set<String> primaryNames) {
        if (flyway == null) {
            throw new IllegalArgumentException("Flyway targets must not be null");
        }
        Map<String, ShardingDataSourceProperties.PhysicalDataSourceProperties> byName =
                dataSources.stream().collect(Collectors.toMap(
                        ShardingDataSourceProperties.PhysicalDataSourceProperties::name,
                        source -> source));
        Set<String> targetNames = new LinkedHashSet<>();
        for (ShardingDataSourceProperties.FlywayTargetProperties target : flyway.targets()) {
            if (target == null
                    || target.dataSourceName() == null
                    || target.dataSourceName().isBlank()) {
                throw new IllegalArgumentException("Flyway target name must not be blank");
            }
            if (!targetNames.add(target.dataSourceName())) {
                throw new IllegalArgumentException(
                        "duplicate Flyway target: " + target.dataSourceName());
            }
            ShardingDataSourceProperties.PhysicalDataSourceProperties source =
                    byName.get(target.dataSourceName());
            if (source == null) {
                throw new IllegalArgumentException(
                        "Flyway target is not a physical data source: "
                                + target.dataSourceName());
            }
            if (source.role() != ShardingDataSourceProperties.DataSourceRole.PRIMARY) {
                throw new IllegalArgumentException(
                        "Flyway target must not reference a replica: "
                                + target.dataSourceName());
            }
            if (target.locations().isEmpty()
                    || target.locations().stream()
                            .anyMatch(location -> location == null || location.isBlank())) {
                throw new IllegalArgumentException(
                        "Flyway target locations must not be empty: "
                                + target.dataSourceName());
            }
        }
        if (!targetNames.equals(primaryNames)) {
            Set<String> missing = new LinkedHashSet<>(primaryNames);
            missing.removeAll(targetNames);
            throw new IllegalArgumentException(
                    "every primary must have exactly one Flyway target: " + missing);
        }
    }

    private static void validateRuleRouting(
            ShardingDataSourceProperties.ShardingRoutingProperties routing,
            ShardingNodeMap nodeMap,
            Map<String, List<ShardingDataSourceProperties.PhysicalDataSourceProperties>>
                    sourcesByLogicalName,
            byte[] yaml) {
        if (yaml == null || yaml.length == 0) {
            throw new IllegalArgumentException("ShardingSphere rule content must not be empty");
        }
        String content = new String(yaml, StandardCharsets.UTF_8);
        if (content.contains("!SINGLE") || content.contains("defaultDataSource")) {
            throw new IllegalArgumentException(
                    "!SINGLE and defaultDataSource are not allowed; use none strategies");
        }
        String shardingRule = uniqueRuleSection(content, "!SHARDING");
        requireTwice(shardingRule, "node-count", routing.nodeCount());
        requireTwice(shardingRule, "node-map", routing.nodeMap());
        validateDataSourceRules(content, sourcesByLogicalName);
        validateActualDataNodes(shardingRule, nodeMap);
    }

    private static String uniqueRuleSection(String content, String ruleName) {
        List<String> lines = content.lines().toList();
        List<Integer> starts = new java.util.ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).strip().equals("- " + ruleName)) {
                starts.add(index);
            }
        }
        if (starts.size() != 1) {
            throw new IllegalArgumentException(
                    "ShardingSphere rule must contain exactly one " + ruleName);
        }
        int start = starts.getFirst();
        int markerIndent = indentation(lines.get(start));
        int end = lines.size();
        for (int index = start + 1; index < lines.size(); index++) {
            String line = lines.get(index);
            String value = line.strip();
            if (!value.isEmpty()
                    && indentation(line) <= markerIndent
                    && (value.startsWith("- !") || !line.startsWith(" "))) {
                end = index;
                break;
            }
        }
        return String.join("\n", lines.subList(start, end));
    }

    private static void validateDataSourceRules(
            String content,
            Map<String, List<ShardingDataSourceProperties.PhysicalDataSourceProperties>>
                    sourcesByLogicalName) {
        if (!content.contains("- !READWRITE_SPLITTING")) {
            sourcesByLogicalName.forEach((logicalName, sources) -> {
                if (sources.size() != 1
                        || sources.getFirst().role()
                                != ShardingDataSourceProperties.DataSourceRole.PRIMARY
                        || !logicalName.equals(sources.getFirst().name())) {
                    throw new IllegalArgumentException(
                            "primary-only rules must reference one same-name primary: "
                                    + logicalName);
                }
            });
            return;
        }

        Map<String, ReadwriteGroup> groups = parseReadwriteGroups(content);
        if (!groups.keySet().equals(sourcesByLogicalName.keySet())) {
            throw new IllegalArgumentException(
                    "readwrite groups do not match physical logical groups");
        }
        sourcesByLogicalName.forEach((logicalName, sources) -> {
            String primary = sources.stream()
                    .filter(source -> source.role()
                            == ShardingDataSourceProperties.DataSourceRole.PRIMARY)
                    .map(ShardingDataSourceProperties.PhysicalDataSourceProperties::name)
                    .findFirst()
                    .orElseThrow();
            Set<String> replicas = sources.stream()
                    .filter(source -> source.role()
                            == ShardingDataSourceProperties.DataSourceRole.REPLICA)
                    .map(ShardingDataSourceProperties.PhysicalDataSourceProperties::name)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            ReadwriteGroup group = groups.get(logicalName);
            if (!primary.equals(group.writer())) {
                throw new IllegalArgumentException(
                        "readwrite group write data source must be its configured primary: "
                                + logicalName);
            }
            if (replicas.isEmpty() || !replicas.equals(group.readers())) {
                throw new IllegalArgumentException(
                        "readwrite group read data sources must match configured replicas: "
                                + logicalName);
            }
            if (!"PRIMARY".equals(group.transactionalReadQueryStrategy())) {
                throw new IllegalArgumentException(
                        "transactional read query strategy must be PRIMARY: " + logicalName);
            }
        });
    }

    private static Map<String, ReadwriteGroup> parseReadwriteGroups(String content) {
        int start = content.indexOf("    dataSourceGroups:");
        int end = content.indexOf("\n    loadBalancers:", start);
        if (start < 0 || end < 0) {
            throw new IllegalArgumentException(
                    "READWRITE_SPLITTING must define dataSourceGroups and loadBalancers");
        }

        Map<String, ReadwriteGroupBuilder> builders = new LinkedHashMap<>();
        ReadwriteGroupBuilder current = null;
        boolean readingReplicas = false;
        for (String line : content.substring(start, end).lines().toList()) {
            String value = line.strip();
            int indentation = line.length() - line.stripLeading().length();
            if (indentation == 6 && value.endsWith(":")) {
                String groupName = value.substring(0, value.length() - 1);
                current = new ReadwriteGroupBuilder(groupName);
                if (builders.put(groupName, current) != null) {
                    throw new IllegalArgumentException(
                            "duplicate readwrite group: " + groupName);
                }
                readingReplicas = false;
            } else if (current != null && indentation == 8) {
                readingReplicas = false;
                if (value.startsWith("writeDataSourceName:")) {
                    current.writer = scalar(value);
                } else if (value.equals("readDataSourceNames:")) {
                    readingReplicas = true;
                } else if (value.startsWith("transactionalReadQueryStrategy:")) {
                    current.transactionalReadQueryStrategy = scalar(value);
                }
            } else if (current != null
                    && readingReplicas
                    && indentation == 10
                    && value.startsWith("- ")) {
                String replica = value.substring(2).trim();
                if (!current.readers.add(replica)) {
                    throw new IllegalArgumentException(
                            "duplicate read data source: " + replica);
                }
            }
        }
        return builders.values().stream().collect(Collectors.toMap(
                builder -> builder.name,
                ReadwriteGroupBuilder::build,
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private static String scalar(String line) {
        int separator = line.indexOf(':');
        String value = separator < 0 ? "" : line.substring(separator + 1).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("rule scalar value must not be blank: " + line);
        }
        return value;
    }

    private static void validateActualDataNodes(String shardingRule, ShardingNodeMap nodeMap) {
        Set<String> expectedDatabases = nodeMap.nodes().values().stream()
                .map(ShardingNodeMap.PhysicalNode::database)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> expectedTableSuffixes = nodeMap.nodes().values().stream()
                .map(ShardingNodeMap.PhysicalNode::tableSuffix)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, TableRule> tableRules = parseShardingTableRules(shardingRule);
        boolean shardedTablePresent = false;
        for (Map.Entry<String, TableRule> entry : tableRules.entrySet()) {
            String logicalTable = entry.getKey();
            TableRule tableRule = entry.getValue();
            String expression = tableRule.actualDataNodes();
            String[] segments = expression.split("\\.public\\.", 2);
            if (segments.length != 2) {
                throw new IllegalArgumentException(
                        "actualDataNodes do not match stable node map: " + expression);
            }

            if (segments[0].equals("master_data")) {
                if (!segments[1].equals(logicalTable)) {
                    throw new IllegalArgumentException(
                            "master-data physical table must match logical table: "
                                    + logicalTable);
                }
                if (tableRule.databaseStrategy() != Strategy.NONE
                        || tableRule.tableStrategy() != Strategy.NONE) {
                    throw new IllegalArgumentException(
                            "master-data table must use databaseStrategy.none and "
                                    + "tableStrategy.none: " + logicalTable);
                }
                if (tableRule.shardingAuditRequired()) {
                    throw new IllegalArgumentException(
                            "master-data none table must not require sharding audit: "
                                    + logicalTable);
                }
                continue;
            }

            shardedTablePresent = true;
            if (!expectedDatabases.equals(expandNames(segments[0]))
                    || !expectedTableSuffixes.equals(expandNumericSuffixes(segments[1]))) {
                throw new IllegalArgumentException(
                        "actualDataNodes do not match stable node map: " + expression);
            }
            if (!logicalTable.equals(physicalTableBaseName(segments[1]))) {
                throw new IllegalArgumentException(
                        "actualDataNodes physical table must match logical table: "
                                + logicalTable);
            }
            if (tableRule.databaseStrategy() != Strategy.STANDARD
                    || tableRule.tableStrategy() != Strategy.STANDARD) {
                throw new IllegalArgumentException(
                        "sharded table must use standard database and table strategies: "
                                + logicalTable);
            }
            if (!tableRule.shardingAuditRequired()) {
                throw new IllegalArgumentException(
                        "sharded table must declare DML sharding audit: " + logicalTable);
            }
            if (tableRule.hintDisableAllowed()) {
                throw new IllegalArgumentException(
                        "sharded table allowHintDisable must be false: " + logicalTable);
            }
        }
        if (shardedTablePresent) {
            validateDmlAuditor(shardingRule);
        }
    }

    private static Map<String, TableRule> parseShardingTableRules(String shardingRule) {
        Map<String, List<String>> blocks = new LinkedHashMap<>();
        List<String> currentBlock = null;
        boolean insideTables = false;
        for (String line : shardingRule.lines().toList()) {
            String value = line.strip();
            int indentation = indentation(line);
            if (indentation == 4 && value.equals("tables:")) {
                insideTables = true;
                continue;
            }
            if (insideTables && indentation <= 4 && !value.isEmpty()) {
                break;
            }
            if (insideTables && indentation == 6 && value.endsWith(":")) {
                String table = value.substring(0, value.length() - 1);
                if (!table.matches("[a-zA-Z0-9_]+") || blocks.containsKey(table)) {
                    throw new IllegalArgumentException(
                            "invalid or duplicate SHARDING logical table: " + table);
                }
                currentBlock = new java.util.ArrayList<>();
                blocks.put(table, currentBlock);
            } else if (insideTables && currentBlock != null) {
                currentBlock.add(line);
            }
        }
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException(
                    "SHARDING rules must define actualDataNodes by logical table");
        }
        return blocks.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> parseTableRule(entry.getKey(), entry.getValue()),
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private static TableRule parseTableRule(String table, List<String> lines) {
        List<String> actualDataNodes = lines.stream()
                .map(String::strip)
                .filter(line -> line.startsWith("actualDataNodes:"))
                .map(ShardingTopologyValidator::scalar)
                .toList();
        if (actualDataNodes.size() != 1) {
            throw new IllegalArgumentException(
                    "SHARDING logical table must define one actualDataNodes: " + table);
        }
        Strategy databaseStrategy = parseStrategy(lines, "databaseStrategy", table);
        Strategy tableStrategy = parseStrategy(lines, "tableStrategy", table);
        boolean auditRequired = lines.stream()
                .map(String::strip)
                .anyMatch("- sharding_key_required_auditor"::equals);
        List<String> allowHintDisable = lines.stream()
                .map(String::strip)
                .filter(line -> line.startsWith("allowHintDisable:"))
                .map(ShardingTopologyValidator::scalar)
                .toList();
        if (auditRequired && allowHintDisable.size() != 1) {
            throw new IllegalArgumentException(
                    "sharded table audit must declare allowHintDisable: " + table);
        }
        if (allowHintDisable.size() > 1
                || (!allowHintDisable.isEmpty()
                        && !Set.of("true", "false").contains(allowHintDisable.getFirst()))) {
            throw new IllegalArgumentException(
                    "allowHintDisable must be one boolean: " + table);
        }
        boolean hintDisableAllowed = !allowHintDisable.isEmpty()
                && Boolean.parseBoolean(allowHintDisable.getFirst());
        return new TableRule(
                actualDataNodes.getFirst(),
                databaseStrategy,
                tableStrategy,
                auditRequired,
                hintDisableAllowed);
    }

    private static Strategy parseStrategy(
            List<String> lines,
            String strategyName,
            String table) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (indentation(line) == 8 && line.strip().equals(strategyName + ":")) {
                for (int nested = index + 1; nested < lines.size(); nested++) {
                    String nestedLine = lines.get(nested);
                    if (nestedLine.isBlank()) {
                        continue;
                    }
                    if (indentation(nestedLine) != 10) {
                        break;
                    }
                    return switch (nestedLine.strip()) {
                        case "none:" -> Strategy.NONE;
                        case "standard:" -> Strategy.STANDARD;
                        default -> throw new IllegalArgumentException(
                                "unsupported " + strategyName + " for table " + table);
                    };
                }
            }
        }
        throw new IllegalArgumentException(
                "table must declare " + strategyName + ": " + table);
    }

    private static void validateDmlAuditor(String shardingRule) {
        long auditorTypes = shardingRule.lines()
                .map(String::strip)
                .filter("type: DML_SHARDING_CONDITIONS"::equals)
                .count();
        if (!shardingRule.contains("sharding_key_required_auditor:")
                || auditorTypes != 1) {
            throw new IllegalArgumentException(
                    "sharded tables require one DML_SHARDING_CONDITIONS auditor");
        }
    }

    private static String physicalTableBaseName(String expression) {
        Matcher matcher = INLINE_RANGE.matcher(expression);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            if (!prefix.endsWith("_")) {
                throw new IllegalArgumentException(
                        "actualDataNodes physical table range must use a numeric suffix: "
                                + expression);
            }
            return prefix.substring(0, prefix.length() - 1);
        }
        int separator = expression.lastIndexOf('_');
        if (separator < 1
                || separator == expression.length() - 1
                || !expression.substring(separator + 1).matches("\\d+")) {
            throw new IllegalArgumentException(
                    "actualDataNodes table must end with a numeric suffix: " + expression);
        }
        return expression.substring(0, separator);
    }

    private static Set<String> expandNames(String expression) {
        Matcher matcher = INLINE_RANGE.matcher(expression);
        if (!matcher.matches()) {
            if (!expression.matches("[a-zA-Z0-9_]+")) {
                throw new IllegalArgumentException(
                        "unsupported actualDataNodes data source expression: " + expression);
            }
            return Set.of(expression);
        }
        int start = Integer.parseInt(matcher.group(2));
        int end = Integer.parseInt(matcher.group(3));
        if (start > end) {
            throw new IllegalArgumentException(
                    "actualDataNodes range must be ascending: " + expression);
        }
        Set<String> values = new LinkedHashSet<>();
        for (int value = start; value <= end; value++) {
            values.add(matcher.group(1) + value);
        }
        return values;
    }

    private static Set<Integer> expandNumericSuffixes(String expression) {
        Matcher matcher = INLINE_RANGE.matcher(expression);
        if (matcher.matches()) {
            int start = Integer.parseInt(matcher.group(2));
            int end = Integer.parseInt(matcher.group(3));
            if (start > end) {
                throw new IllegalArgumentException(
                        "actualDataNodes table range must be ascending: " + expression);
            }
            Set<Integer> values = new LinkedHashSet<>();
            for (int value = start; value <= end; value++) {
                values.add(value);
            }
            return values;
        }
        int separator = expression.lastIndexOf('_');
        if (separator < 0 || separator == expression.length() - 1) {
            throw new IllegalArgumentException(
                    "actualDataNodes table must end with a numeric suffix: " + expression);
        }
        try {
            return Set.of(Integer.parseInt(expression.substring(separator + 1)));
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "actualDataNodes table must end with a numeric suffix: " + expression);
        }
    }

    private static void requireTwice(String content, String name, Object expectedValue) {
        String expected = name + ": " + expectedValue;
        long occurrences = content.lines()
                .map(String::strip)
                .filter(expected::equals)
                .count();
        if (occurrences != 2) {
            throw new IllegalArgumentException(
                    "database and table algorithms must share routing property: " + expected);
        }
    }

    private static int indentation(String line) {
        return line.length() - line.stripLeading().length();
    }

    private record TableRule(
            String actualDataNodes,
            Strategy databaseStrategy,
            Strategy tableStrategy,
            boolean shardingAuditRequired,
            boolean hintDisableAllowed) {
    }

    private enum Strategy {
        NONE,
        STANDARD
    }

    private record ReadwriteGroup(
            String writer,
            Set<String> readers,
            String transactionalReadQueryStrategy) {
    }

    private static final class ReadwriteGroupBuilder {

        private final String name;
        private final Set<String> readers = new LinkedHashSet<>();
        private String writer;
        private String transactionalReadQueryStrategy;

        private ReadwriteGroupBuilder(String name) {
            this.name = name;
        }

        private ReadwriteGroup build() {
            if (writer == null || transactionalReadQueryStrategy == null) {
                throw new IllegalArgumentException(
                        "readwrite group is incomplete: " + name);
            }
            return new ReadwriteGroup(
                    writer,
                    Set.copyOf(readers),
                    transactionalReadQueryStrategy);
        }
    }
}
