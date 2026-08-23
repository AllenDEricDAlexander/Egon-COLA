package ${package}.infrastructure.config.datasource;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/**
 * Binds only the physical topology selected by the current data source mode.
 */
public final class ShardingDataSourcePropertiesLoader {

    private static final String ROUTING_PREFIX = "app.sharding.routing";
    private static final String TOPOLOGY_PREFIX = "app.";

    private final Binder binder;

    public ShardingDataSourcePropertiesLoader(Environment environment) {
        this.binder = Binder.get(environment);
    }

    public ShardingDataSourceProperties load(DataSourceModeProperties modeProperties) {
        String topologyName = modeProperties.mode().topologyName();
        ShardingDataSourceProperties topology = binder
                .bind(
                        TOPOLOGY_PREFIX + topologyName,
                        Bindable.of(ShardingDataSourceProperties.class))
                .orElseThrow(() -> new IllegalStateException(
                        "Missing ShardingSphere topology: " + topologyName));
        ShardingDataSourceProperties.ShardingRoutingProperties routing = binder
                .bind(
                        ROUTING_PREFIX,
                        Bindable.of(
                                ShardingDataSourceProperties
                                        .ShardingRoutingProperties.class))
                .orElseThrow(() -> new IllegalStateException(
                        "Missing ShardingSphere routing configuration"));
        return new ShardingDataSourceProperties(
                topology.config(),
                routing,
                topology.physicalDataSources(),
                topology.flyway());
    }
}
