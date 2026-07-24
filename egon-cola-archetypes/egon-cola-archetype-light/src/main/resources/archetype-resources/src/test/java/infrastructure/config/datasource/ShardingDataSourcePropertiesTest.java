package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ShardingDataSourcePropertiesTest {

    @Test
    void shouldBindPhysicalDataSourcesRoutingAndFlywayTargets() {
        Map<String, Object> values = Map.ofEntries(
                Map.entry("app.sharding.config", "classpath:sharding/rules.yml"),
                Map.entry("app.sharding.routing.mapping-version", "1"),
                Map.entry("app.sharding.routing.node-count", "4"),
                Map.entry(
                        "app.sharding.routing.node-map",
                        "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1"),
                Map.entry("app.sharding.physical-data-sources[0].name", "single"),
                Map.entry("app.sharding.physical-data-sources[0].logical-name", "single"),
                Map.entry("app.sharding.physical-data-sources[0].role", "PRIMARY"),
                Map.entry(
                        "app.sharding.physical-data-sources[0].driver-class-name",
                        "org.h2.Driver"),
                Map.entry(
                        "app.sharding.physical-data-sources[0].jdbc-url",
                        "jdbc:h2:mem:single"),
                Map.entry("app.sharding.physical-data-sources[0].username", "sa"),
                Map.entry("app.sharding.physical-data-sources[0].password", "secret"),
                Map.entry(
                        "app.sharding.flyway.targets[0].data-source-name",
                        "single"),
                Map.entry(
                        "app.sharding.flyway.targets[0].locations[0]",
                        "classpath:db/migration/sharding/single"));

        ShardingDataSourceProperties properties = new Binder(
                        new MapConfigurationPropertySource(values))
                .bind("app.sharding", Bindable.of(ShardingDataSourceProperties.class))
                .get();

        assertThat(properties.config()).isEqualTo("classpath:sharding/rules.yml");
        assertThat(properties.routing().nodeCount()).isEqualTo(4);
        assertThat(properties.physicalDataSources()).singleElement()
                .satisfies(dataSource -> {
                    assertThat(dataSource.name()).isEqualTo("single");
                    assertThat(dataSource.role())
                            .isEqualTo(ShardingDataSourceProperties.DataSourceRole.PRIMARY);
                    assertThat(dataSource.toString()).doesNotContain("secret");
                });
        assertThat(properties.flyway().targets()).singleElement()
                .satisfies(target -> assertThat(target.locations())
                        .containsExactly("classpath:db/migration/sharding/single"));
    }
}
