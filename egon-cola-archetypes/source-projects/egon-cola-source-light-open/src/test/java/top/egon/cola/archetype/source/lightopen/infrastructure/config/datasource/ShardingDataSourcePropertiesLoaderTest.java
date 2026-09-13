package top.egon.cola.archetype.source.lightopen.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class ShardingDataSourcePropertiesLoaderTest {
    private ValidatorFactory factory;
    private ValidationUtils validation;

    @BeforeEach void prepare() {
        factory = Validation.buildDefaultValidatorFactory();
        validation = new ValidationUtils(factory.getValidator());
    }

    @AfterEach void close() { factory.close(); }


    @Test
    void shouldBindOnlyTheTopologySelectedByMode() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("app.sharding.config", "classpath:sharding/primary.yml");
        values.put("app.sharding.routing.node-count", "4");
        values.put(
                "app.sharding.routing.node-map",
                "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1");
        values.put("app.sharding.physical-data-sources[0].name", "master_data");
        values.put("app.sharding.physical-data-sources[0].logical-name", "master_data");
        values.put("app.sharding.physical-data-sources[0].role", "PRIMARY");
        values.put(
                "app.sharding.physical-data-sources[0].driver-class-name",
                "org.postgresql.Driver");
        values.put(
                "app.sharding.physical-data-sources[0].jdbc-url",
                "jdbc:postgresql://localhost:5432/master");
        values.put("app.sharding.physical-data-sources[0].username", "sa");
        values.put("app.sharding.physical-data-sources[0].password", "");
        values.put(
                "app.sharding.ddl.targets[0].data-source-name",
                "master_data");
        values.put(
                "app.sharding.ddl.targets[0].manifest",
                "classpath:db/egon-mp/manifest.json");
        values.put("app.sharding.ddl.targets[0].schema", "public");
        values.put("app.sharding.ddl.targets[0].role", "MASTER_DATA");
        values.put(
                "app.sharding-readwrite.physical-data-sources[0].jdbc-url",
                "${MISSING_READWRITE_URL}");
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("test", values));

        ShardingDataSourceProperties properties =
                new ShardingDataSourcePropertiesLoader(environment, validation)
                        .load(new DataSourceModeProperties(
                                DataSourceModeProperties.DataSourceMode.SHARDING));

        assertThat(properties.config())
                .isEqualTo("classpath:sharding/primary.yml");
        assertThat(properties.routing().nodeCount()).isEqualTo(4);
        assertThat(properties.physicalDataSources()).singleElement()
                .extracting(ShardingDataSourceProperties
                        .PhysicalDataSourceProperties::name)
                .isEqualTo("master_data");
    }

    @Test
    void shouldBindReadwriteTopologyWithoutResolvingInactiveShardingTopology() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("app.sharding.config", "${MISSING_SHARDING_CONFIG}");
        values.put("app.sharding.routing.node-count", "4");
        values.put(
                "app.sharding.routing.node-map",
                "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1");
        values.put(
                "app.sharding-readwrite.config",
                "classpath:sharding/readwrite.yml");
        values.put(
                "app.sharding-readwrite.physical-data-sources[0].name",
                "master_data_primary");
        values.put(
                "app.sharding-readwrite.physical-data-sources[0].logical-name",
                "master_data");
        values.put(
                "app.sharding-readwrite.physical-data-sources[0].role",
                "PRIMARY");
        values.put(
                "app.sharding-readwrite.physical-data-sources[0].driver-class-name",
                "org.postgresql.Driver");
        values.put(
                "app.sharding-readwrite.physical-data-sources[0].jdbc-url",
                "jdbc:postgresql://localhost:5432/master_primary");
        values.put(
                "app.sharding-readwrite.physical-data-sources[0].username",
                "sa");
        values.put(
                "app.sharding-readwrite.physical-data-sources[0].password",
                "");
        values.put(
                "app.sharding-readwrite.ddl.targets[0].data-source-name",
                "master_data_primary");
        values.put(
                "app.sharding-readwrite.ddl.targets[0].manifest",
                "classpath:db/egon-mp/manifest.json");
        values.put("app.sharding-readwrite.ddl.targets[0].schema", "public");
        values.put("app.sharding-readwrite.ddl.targets[0].role", "MASTER_DATA");
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(
                new MapPropertySource("test", values));

        ShardingDataSourceProperties properties =
                new ShardingDataSourcePropertiesLoader(environment, validation)
                        .load(new DataSourceModeProperties(
                                DataSourceModeProperties.DataSourceMode
                                        .SHARDING_READWRITE));

        assertThat(properties.config())
                .isEqualTo("classpath:sharding/readwrite.yml");
        assertThat(properties.routing().nodeCount()).isEqualTo(4);
        assertThat(properties.physicalDataSources()).singleElement()
                .extracting(ShardingDataSourceProperties
                        .PhysicalDataSourceProperties::name)
                .isEqualTo("master_data_primary");
    }
}
