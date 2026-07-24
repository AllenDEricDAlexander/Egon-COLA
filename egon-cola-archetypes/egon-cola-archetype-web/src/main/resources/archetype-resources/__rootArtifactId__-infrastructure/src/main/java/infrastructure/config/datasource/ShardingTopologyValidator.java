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
        expectedLogicalNames.add("single");
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
        values.setProperty("mapping-version", Integer.toString(routing.mappingVersion()));
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
        String shardingRule = uniqueRuleSection(content, "!SHARDING");
        String singleRule = uniqueRuleSection(content, "!SINGLE");
        requireTwice(shardingRule, "mapping-version", routing.mappingVersion());
        requireTwice(shardingRule, "node-count", routing.nodeCount());
        requireTwice(shardingRule, "node-map", routing.nodeMap());
        validateDataSourceRules(content, sourcesByLogicalName);
        validateActualDataNodes(shardingRule, nodeMap);
        validateSingleRule(singleRule);
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
        Map<String, String> tableRules = parseShardingTableRules(shardingRule);
        tableRules.forEach((logicalTable, expression) -> {
            String[] segments = expression.split("\\.public\\.", 2);
            if (segments.length != 2
                    || !expectedDatabases.equals(expandNames(segments[0]))
                    || !expectedTableSuffixes.equals(expandNumericSuffixes(segments[1]))) {
                throw new IllegalArgumentException(
                        "actualDataNodes do not match stable node map: " + expression);
            }
            if (!logicalTable.equals(physicalTableBaseName(segments[1]))) {
                throw new IllegalArgumentException(
                        "actualDataNodes physical table must match logical table: "
                                + logicalTable);
            }
        });
    }

    private static Map<String, String> parseShardingTableRules(String shardingRule) {
        Map<String, String> result = new LinkedHashMap<>();
        Set<String> declaredTables = new LinkedHashSet<>();
        String currentTable = null;
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
                if (currentTable != null && !result.containsKey(currentTable)) {
                    throw new IllegalArgumentException(
                            "SHARDING logical table must define actualDataNodes: "
                                    + currentTable);
                }
                currentTable = value.substring(0, value.length() - 1);
                if (!currentTable.matches("[a-zA-Z0-9_]+")
                        || !declaredTables.add(currentTable)) {
                    throw new IllegalArgumentException(
                            "invalid or duplicate SHARDING logical table: " + currentTable);
                }
            } else if (insideTables
                    && currentTable != null
                    && indentation == 8
                    && value.startsWith("actualDataNodes:")) {
                result.put(currentTable, scalar(value));
            }
        }
        if (currentTable != null && !result.containsKey(currentTable)) {
            throw new IllegalArgumentException(
                    "SHARDING logical table must define actualDataNodes: " + currentTable);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "SHARDING rules must define actualDataNodes by logical table");
        }
        return result;
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

    private static void validateSingleRule(String singleRule) {
        if (!"single".equals(uniqueScalar(singleRule, "defaultDataSource"))) {
            throw new IllegalArgumentException(
                    "SINGLE defaultDataSource must reference logical group single");
        }
        Set<String> tables = new LinkedHashSet<>();
        boolean insideTables = false;
        for (String line : singleRule.lines().toList()) {
            String value = line.strip();
            int indentation = indentation(line);
            if (indentation == 4 && value.equals("tables:")) {
                insideTables = true;
                continue;
            }
            if (insideTables && indentation <= 4 && !value.isEmpty()) {
                break;
            }
            if (insideTables && indentation == 6 && value.startsWith("- ")) {
                String table = value.substring(2).trim();
                if (!table.matches("single\\.public\\.[a-zA-Z0-9_]+")) {
                    throw new IllegalArgumentException(
                            "SINGLE table must reference the single logical data source: "
                                    + table);
                }
                if (!tables.add(table)) {
                    throw new IllegalArgumentException("duplicate SINGLE table: " + table);
                }
            }
        }
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("SINGLE rule must define tables");
        }
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

    private static String uniqueScalar(String content, String name) {
        List<String> values = content.lines()
                .map(String::strip)
                .filter(line -> line.startsWith(name + ":"))
                .map(ShardingTopologyValidator::scalar)
                .toList();
        if (values.size() != 1) {
            throw new IllegalArgumentException(
                    "rule must define exactly one " + name);
        }
        return values.getFirst();
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
