package top.egon.cola.archetype.source.webopen.infrastructure.config.datasource;

import java.util.Map;
import javax.sql.DataSource;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;

/**
 * Facade for physical pool creation, topology validation and logical startup.
 */
public final class ShardingDataSourceBootstrapper {

    private final PhysicalDataSourceFactory physicalDataSourceFactory;
    private final ShardingYamlLoader shardingYamlLoader;
    private final ShardingTopologyValidator topologyValidator;
    private final LogicalDataSourceFactory logicalDataSourceFactory;

    public ShardingDataSourceBootstrapper(
            PhysicalDataSourceFactory physicalDataSourceFactory,
            ShardingYamlLoader shardingYamlLoader,
            ShardingTopologyValidator topologyValidator) {
        this(
                physicalDataSourceFactory,
                shardingYamlLoader,
                topologyValidator,
                YamlShardingSphereDataSourceFactory::createDataSource);
    }

    ShardingDataSourceBootstrapper(
            PhysicalDataSourceFactory physicalDataSourceFactory,
            ShardingYamlLoader shardingYamlLoader,
            ShardingTopologyValidator topologyValidator,
            LogicalDataSourceFactory logicalDataSourceFactory) {
        this.physicalDataSourceFactory = physicalDataSourceFactory;
        this.shardingYamlLoader = shardingYamlLoader;
        this.topologyValidator = topologyValidator;
        this.logicalDataSourceFactory = logicalDataSourceFactory;
    }

    public DataSource createDataSource(ShardingDataSourceProperties properties) {
        Map<String, DataSource> physicalDataSources =
                physicalDataSourceFactory.create(properties);
        try {
            byte[] yaml = shardingYamlLoader.load(properties.config());
            topologyValidator.validate(properties, yaml);
            return logicalDataSourceFactory.create(physicalDataSources, yaml);
        } catch (RuntimeException failure) {
            physicalDataSourceFactory.close(physicalDataSources.values());
            throw failure;
        } catch (Exception failure) {
            physicalDataSourceFactory.close(physicalDataSources.values());
            throw new IllegalStateException(
                    "Unable to create the ShardingSphere logical data source ("
                            + failure.getClass().getSimpleName()
                            + ")",
                    failure);
        }
    }

    @FunctionalInterface
    interface LogicalDataSourceFactory {

        DataSource create(Map<String, DataSource> physicalDataSources, byte[] yaml)
                throws Exception;
    }
}
