package ${package}.infrastructure.config.datasource;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;

/**
 * Runs Flyway against every configured primary before logical data source creation.
 */
public class PhysicalDataSourceFlywayMigrator {

    private final TargetMigrator targetMigrator;

    public PhysicalDataSourceFlywayMigrator() {
        this(PhysicalDataSourceFlywayMigrator::migrateOne);
    }

    PhysicalDataSourceFlywayMigrator(TargetMigrator targetMigrator) {
        this.targetMigrator = targetMigrator;
    }

    public void migrate(
            Map<String, DataSource> physicalDataSources,
            List<ShardingDataSourceProperties.FlywayTargetProperties> targets,
            FlywayProperties springFlywayProperties) {
        if (physicalDataSources == null
                || targets == null
                || springFlywayProperties == null) {
            throw new IllegalArgumentException("Flyway migration arguments must not be null");
        }
        targets.stream()
                .sorted(Comparator.comparing(
                        ShardingDataSourceProperties.FlywayTargetProperties::dataSourceName))
                .forEach(target -> migrateTarget(
                        physicalDataSources,
                        target,
                        springFlywayProperties));
    }

    private void migrateTarget(
            Map<String, DataSource> physicalDataSources,
            ShardingDataSourceProperties.FlywayTargetProperties target,
            FlywayProperties springFlywayProperties) {
        DataSource dataSource = physicalDataSources.get(target.dataSourceName());
        if (dataSource == null) {
            throw migrationFailure(target, "target data source does not exist");
        }
        try {
            targetMigrator.migrate(dataSource, target, springFlywayProperties);
        } catch (RuntimeException failure) {
            throw migrationFailure(target, failure.getClass().getSimpleName());
        }
    }

    private static void migrateOne(
            DataSource dataSource,
            ShardingDataSourceProperties.FlywayTargetProperties target,
            FlywayProperties properties) {
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations(target.locations().toArray(String[]::new))
                .baselineOnMigrate(properties.isBaselineOnMigrate())
                .validateOnMigrate(properties.isValidateOnMigrate())
                .validateMigrationNaming(properties.isValidateMigrationNaming())
                .cleanDisabled(properties.isCleanDisabled())
                .encoding(properties.getEncoding())
                .placeholders(properties.getPlaceholders());
        Flyway flyway = configuration.load();
        flyway.migrate();
        flyway.validate();
    }

    private static IllegalStateException migrationFailure(
            ShardingDataSourceProperties.FlywayTargetProperties target,
            String reason) {
        return new IllegalStateException(
                "Flyway migration failed for target "
                        + target.dataSourceName()
                        + " at locations "
                        + target.locations()
                        + " ("
                        + reason
                        + ")");
    }

    @FunctionalInterface
    interface TargetMigrator {

        void migrate(
                DataSource dataSource,
                ShardingDataSourceProperties.FlywayTargetProperties target,
                FlywayProperties properties);
    }
}
