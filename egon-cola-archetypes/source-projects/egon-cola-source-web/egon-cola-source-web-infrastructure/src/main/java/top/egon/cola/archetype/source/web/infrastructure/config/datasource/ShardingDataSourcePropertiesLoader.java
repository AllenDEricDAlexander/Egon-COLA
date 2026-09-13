package top.egon.cola.archetype.source.web.infrastructure.config.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Binds and validates only the topology selected by the current datasource mode. */
@Slf4j
@RequiredArgsConstructor
public final class ShardingDataSourcePropertiesLoader {
    @Qualifier("environment")
    private final Environment environment;
    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    public ShardingDataSourceProperties load(DataSourceModeProperties mode) {
        Binder binder = Binder.get(environment);
        var topology = binder.bind("app." + mode.mode().topologyName(), Bindable.of(ShardingDataSourceProperties.class))
                .orElseThrow(() -> new IllegalStateException("SHARDING_TOPOLOGY_MISSING"));
        var routing = binder.bind("app.sharding.routing", Bindable.of(ShardingDataSourceProperties.ShardingRoutingProperties.class))
                .orElseThrow(() -> new IllegalStateException("SHARDING_ROUTING_MISSING"));
        var checked = validation.validate(new ShardingDataSourceProperties(topology.config(), routing, topology.physicalDataSources(), topology.ddl()));
        java.util.Map<String, String> schemas = new java.util.HashMap<>();
        for (var target : checked.ddl().targets()) {
            checked.physicalDataSources().stream().filter(source -> source.name().equals(target.dataSourceName()))
                    .findFirst().ifPresent(source -> schemas.put(source.logicalName(), target.schema()));
        }
        var sources = checked.physicalDataSources().stream().map(source -> {
            String schema = schemas.get(source.logicalName());
            if (schema == null) { throw new IllegalArgumentException("DDL_PRIMARY_COVERAGE_REQUIRED"); }
            String url = source.jdbcUrl();
            String query = java.net.URI.create(url.substring("jdbc:".length())).getRawQuery();
            boolean configured = false;
            if (query != null) {
                for (String pair : query.split("&")) {
                    String[] entry = pair.split("=", 2);
                    if ("currentSchema".equals(entry[0])) {
                        if (configured || entry.length != 2 || !schema.equals(java.net.URLDecoder.decode(entry[1], java.nio.charset.StandardCharsets.UTF_8))) {
                            throw new IllegalArgumentException("JDBC_SCHEMA_MISMATCH");
                        }
                        configured = true;
                    }
                }
            }
            if (!configured) { url += (url.contains("?") ? "&" : "?") + "currentSchema=" + schema; }
            return new ShardingDataSourceProperties.PhysicalDataSourceProperties(source.name(), source.logicalName(), source.role(),
                    source.driverClassName(), url, source.username(), source.password());
        }).toList();
        return new ShardingDataSourceProperties(checked.config(), routing, sources, checked.ddl());
    }
}
