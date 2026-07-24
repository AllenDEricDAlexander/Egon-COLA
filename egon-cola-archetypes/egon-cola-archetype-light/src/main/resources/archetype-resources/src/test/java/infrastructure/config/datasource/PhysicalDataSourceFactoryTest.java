package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class PhysicalDataSourceFactoryTest {

    @Test
    void shouldCreateNamedPoolsInConfigurationOrder() {
        PhysicalDataSourceFactory factory = new PhysicalDataSourceFactory();

        Map<String, DataSource> result = factory.create(properties(List.of(
                physical("single", "single"),
                physical("shard_0", "shard_0"))));

        assertThat(result.keySet()).containsExactly("single", "shard_0");
        assertThat(result.get("single")).isInstanceOfSatisfying(
                HikariDataSource.class,
                pool -> {
                    assertThat(pool.getPoolName()).isEqualTo("sharding-single");
                    assertThat(pool.getJdbcUrl()).isEqualTo("jdbc:h2:mem:single");
                });
        factory.close(result.values());
    }

    @Test
    void shouldCloseCreatedPoolsWhenLaterConfigurationFails() {
        HikariDataSource first = new HikariDataSource();
        PhysicalDataSourceFactory factory = new PhysicalDataSourceFactory(properties -> first);

        assertThatThrownBy(() -> factory.create(properties(List.of(
                        physical("single", "single"),
                        physical("single", "shard_0")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");

        assertThat(first.isClosed()).isTrue();
    }

    @Test
    void shouldRejectMissingConnectionPropertyWithoutLeakingPassword() {
        ShardingDataSourceProperties.PhysicalDataSourceProperties invalid =
                new ShardingDataSourceProperties.PhysicalDataSourceProperties(
                        "single",
                        "single",
                        ShardingDataSourceProperties.DataSourceRole.PRIMARY,
                        "org.h2.Driver",
                        null,
                        "sa",
                        "top-secret");

        assertThatThrownBy(() -> new PhysicalDataSourceFactory()
                        .create(properties(List.of(invalid))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("single")
                .hasMessageNotContaining("top-secret");
    }

    private static ShardingDataSourceProperties.PhysicalDataSourceProperties physical(
            String name,
            String logicalName) {
        return new ShardingDataSourceProperties.PhysicalDataSourceProperties(
                name,
                logicalName,
                ShardingDataSourceProperties.DataSourceRole.PRIMARY,
                "org.h2.Driver",
                "jdbc:h2:mem:" + name,
                "sa",
                "secret");
    }

    private static ShardingDataSourceProperties properties(
            List<ShardingDataSourceProperties.PhysicalDataSourceProperties> dataSources) {
        return new ShardingDataSourceProperties(
                "classpath:rules.yml",
                new ShardingDataSourceProperties.ShardingRoutingProperties(
                        1,
                        4,
                        "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1"),
                dataSources,
                new ShardingDataSourceProperties.ShardingFlywayProperties(List.of()));
    }
}
