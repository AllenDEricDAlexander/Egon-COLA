package ${package}.infrastructure.config.datasource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

/**
 * Prevents Spring Boot from migrating the ShardingSphere logical data source.
 *
 * <p>Physical primary data sources are migrated by the bootstrapper before the
 * logical data source is created.
 */
public final class LogicalDataSourceFlywayMigrationStrategy
        implements FlywayMigrationStrategy {

    @Override
    public void migrate(Flyway flyway) {
        // Physical primary migrations have already completed during data source creation.
    }
}
