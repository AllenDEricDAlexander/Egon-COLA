package ${package}.infrastructure.config.datasource;

import java.util.Map;
import javax.sql.DataSource;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;

/**
 * Facade for physical pool creation, topology validation, migration and logical startup.
 */
public final class ShardingDataSourceBootstrapper {

    private final PhysicalDataSourceFactory physicalDataSourceFactory;
    private final ShardingYamlLoader shardingYamlLoader;
    private final ShardingTopologyValidator topologyValidator;
    private final PhysicalDataSourceFlywayMigrator flywayMigrator;
    private final LogicalDataSourceFactory logicalDataSourceFactory;

    public ShardingDataSourceBootstrapper(
            PhysicalDataSourceFactory physicalDataSourceFactory,
            ShardingYamlLoader shardingYamlLoader,
            ShardingTopologyValidator topologyValidator,
            PhysicalDataSourceFlywayMigrator flywayMigrator) {
        this(
                physicalDataSourceFactory,
                shardingYamlLoader,
                topologyValidator,
                flywayMigrator,
                YamlShardingSphereDataSourceFactory::createDataSource);
    }

    ShardingDataSourceBootstrapper(
            PhysicalDataSourceFactory physicalDataSourceFactory,
            ShardingYamlLoader shardingYamlLoader,
            ShardingTopologyValidator topologyValidator,
            PhysicalDataSourceFlywayMigrator flywayMigrator,
            LogicalDataSourceFactory logicalDataSourceFactory) {
        this.physicalDataSourceFactory = physicalDataSourceFactory;
        this.shardingYamlLoader = shardingYamlLoader;
        this.topologyValidator = topologyValidator;
        this.flywayMigrator = flywayMigrator;
        this.logicalDataSourceFactory = logicalDataSourceFactory;
    }

    public DataSource createDataSource(
            ShardingDataSourceProperties properties,
            FlywayProperties flywayProperties) {
        Map<String, DataSource> physicalDataSources =
                physicalDataSourceFactory.create(properties);
        try {
            byte[] yaml = shardingYamlLoader.load(properties.config());
            topologyValidator.validate(properties, yaml);
            flywayMigrator.migrate(
                    physicalDataSources,
                    properties.flyway().targets(),
                    flywayProperties);
            return logicalDataSourceFactory.create(physicalDataSources, yaml);
        } catch (RuntimeException failure) {
            physicalDataSourceFactory.close(physicalDataSources.values());
            throw failure;
        } catch (Exception failure) {
            physicalDataSourceFactory.close(physicalDataSources.values());
            throw new IllegalStateException(
                    "Unable to create the ShardingSphere logical data source ("
                            + failure.getClass().getSimpleName()
                            + ")");
        }
    }

    @FunctionalInterface
    interface LogicalDataSourceFactory {

        DataSource create(Map<String, DataSource> physicalDataSources, byte[] yaml)
                throws Exception;
    }
}
