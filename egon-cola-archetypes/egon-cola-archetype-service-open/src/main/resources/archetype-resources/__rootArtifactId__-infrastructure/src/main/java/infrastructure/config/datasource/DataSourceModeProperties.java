package ${package}.infrastructure.config.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Selects the data source capability independently from the environment profile.
 */
@ConfigurationProperties("app.datasource")
public record DataSourceModeProperties(DataSourceMode mode) {

    public DataSourceModeProperties {
        mode = mode == null ? DataSourceMode.SHARDING : mode;
    }

    public enum DataSourceMode {
        SHARDING("sharding"),
        SHARDING_READWRITE("sharding-readwrite");

        private final String topologyName;

        DataSourceMode(String topologyName) {
            this.topologyName = topologyName;
        }

        public String topologyName() {
            return topologyName;
        }
    }
}
