package ${package}.infrastructure.config.datasource;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Physical topology, stable routing and Flyway targets for sharding mode.
 */
@ConfigurationProperties("app.sharding")
public record ShardingDataSourceProperties(
        String config,
        ShardingRoutingProperties routing,
        List<PhysicalDataSourceProperties> physicalDataSources,
        ShardingFlywayProperties flyway) {

    public ShardingDataSourceProperties {
        physicalDataSources = physicalDataSources == null
                ? List.of()
                : List.copyOf(physicalDataSources);
        flyway = flyway == null ? new ShardingFlywayProperties(List.of()) : flyway;
    }

    public enum DataSourceRole {
        PRIMARY,
        REPLICA
    }

    public record PhysicalDataSourceProperties(
            String name,
            String logicalName,
            DataSourceRole role,
            String driverClassName,
            String jdbcUrl,
            String username,
            String password) {

        @Override
        public String toString() {
            return "PhysicalDataSourceProperties[name=%s, logicalName=%s, role=%s, "
                    + "driverClassName=%s, jdbcUrl=<redacted>, username=<redacted>, "
                    + "password=<redacted>]"
                            .formatted(name, logicalName, role, driverClassName);
        }
    }

    public record ShardingRoutingProperties(
            int mappingVersion,
            int nodeCount,
            String nodeMap) {
    }

    public record FlywayTargetProperties(
            String dataSourceName,
            List<String> locations) {

        public FlywayTargetProperties {
            locations = locations == null ? List.of() : List.copyOf(locations);
        }
    }

    public record ShardingFlywayProperties(List<FlywayTargetProperties> targets) {

        public ShardingFlywayProperties {
            targets = targets == null ? List.of() : List.copyOf(targets);
        }
    }
}
