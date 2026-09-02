package top.egon.cola.archetype.source.serviceopen.infrastructure.config.datasource;

import java.util.List;

/**
 * Physical topology and stable routing for sharding mode.
 */
public record ShardingDataSourceProperties(
        String config,
        ShardingRoutingProperties routing,
        List<PhysicalDataSourceProperties> physicalDataSources) {

    public ShardingDataSourceProperties {
        physicalDataSources = physicalDataSources == null
                ? List.of()
                : List.copyOf(physicalDataSources);
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
            int nodeCount,
            String nodeMap) {
    }

}
