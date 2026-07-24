package ${package}.infrastructure.config.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Selects the data source capability independently from the environment profile.
 */
@ConfigurationProperties("app.datasource")
public record DataSourceModeProperties(DataSourceMode mode) {

    public DataSourceModeProperties {
        mode = mode == null ? DataSourceMode.SINGLE : mode;
    }

    public enum DataSourceMode {
        SINGLE(null),
        SHARDING("sharding"),
        SHARDING_READWRITE("sharding-readwrite");

        private final String topologyName;

        DataSourceMode(String topologyName) {
            this.topologyName = topologyName;
        }

        public boolean isShardingSphere() {
            return this != SINGLE;
        }

        public String topologyName() {
            if (topologyName == null) {
                throw new IllegalStateException(
                        "SINGLE mode does not have a ShardingSphere topology");
            }
            return topologyName;
        }
    }
}
