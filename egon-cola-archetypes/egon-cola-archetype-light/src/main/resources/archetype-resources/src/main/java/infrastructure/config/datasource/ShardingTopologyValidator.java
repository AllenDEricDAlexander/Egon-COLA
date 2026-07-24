package ${package}.infrastructure.config.datasource;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates that physical groups, stable routing and Flyway targets describe one topology.
 */
public final class ShardingTopologyValidator {

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
        validateRuleRouting(properties.routing(), yaml);
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
            byte[] yaml) {
        if (yaml == null || yaml.length == 0) {
            throw new IllegalArgumentException("ShardingSphere rule content must not be empty");
        }
        String content = new String(yaml, StandardCharsets.UTF_8);
        if (!content.contains("- !SHARDING")) {
            throw new IllegalArgumentException("ShardingSphere rule must contain !SHARDING");
        }
        requireTwice(content, "mapping-version: " + routing.mappingVersion());
        requireTwice(content, "node-count: " + routing.nodeCount());
        requireTwice(content, "node-map: " + routing.nodeMap());
    }

    private static void requireTwice(String content, String expected) {
        int occurrences = 0;
        int offset = 0;
        while ((offset = content.indexOf(expected, offset)) >= 0) {
            occurrences++;
            offset += expected.length();
        }
        if (occurrences != 2) {
            throw new IllegalArgumentException(
                    "database and table algorithms must share routing property: " + expected);
        }
    }
}
