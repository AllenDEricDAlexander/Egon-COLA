package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;

class ShardingDataSourceBootstrapperTest {

    @Test
    void shouldCreateLogicalDataSourceAfterValidationAndMigrationUsingSameMap() {
        PhysicalDataSourceFactory physicalFactory = mock(PhysicalDataSourceFactory.class);
        ShardingYamlLoader loader = mock(ShardingYamlLoader.class);
        ShardingTopologyValidator validator = mock(ShardingTopologyValidator.class);
        PhysicalDataSourceFlywayMigrator migrator =
                mock(PhysicalDataSourceFlywayMigrator.class);
        Map<String, DataSource> physical = new LinkedHashMap<>();
        physical.put("single", mock(DataSource.class));
        byte[] yaml = "rules".getBytes();
        DataSource logical = mock(DataSource.class);
        when(physicalFactory.create(any())).thenReturn(physical);
        when(loader.load(any())).thenReturn(yaml);
        ShardingDataSourceBootstrapper.LogicalDataSourceFactory logicalFactory =
                (dataSources, yamlBytes) -> {
                    assertThat(dataSources).isSameAs(physical);
                    assertThat(yamlBytes).isSameAs(yaml);
                    return logical;
                };
        ShardingDataSourceBootstrapper bootstrapper = new ShardingDataSourceBootstrapper(
                physicalFactory,
                loader,
                validator,
                migrator,
                logicalFactory);
        ShardingDataSourceProperties properties =
                ShardingTopologyValidatorTest.validProperties();
        FlywayProperties flywayProperties = new FlywayProperties();

        DataSource result = bootstrapper.createDataSource(properties, flywayProperties);

        assertThat(result).isSameAs(logical);
        InOrder order = inOrder(loader, validator, migrator);
        order.verify(loader).load(properties.config());
        order.verify(validator).validate(properties, yaml);
        order.verify(migrator)
                .migrate(physical, properties.flyway().targets(), flywayProperties);
        verify(physicalFactory, never()).close(any());
    }

    @Test
    void shouldCloseEveryPhysicalPoolAndSkipLogicalCreationWhenMigrationFails() {
        PhysicalDataSourceFactory physicalFactory = mock(PhysicalDataSourceFactory.class);
        ShardingYamlLoader loader = mock(ShardingYamlLoader.class);
        ShardingTopologyValidator validator = mock(ShardingTopologyValidator.class);
        PhysicalDataSourceFlywayMigrator migrator =
                mock(PhysicalDataSourceFlywayMigrator.class);
        Map<String, DataSource> physical = Map.of("single", mock(DataSource.class));
        when(physicalFactory.create(any())).thenReturn(physical);
        when(loader.load(any())).thenReturn(new byte[0]);
        org.mockito.Mockito.doThrow(new IllegalStateException("migration failed"))
                .when(migrator)
                .migrate(any(), any(), any());
        boolean[] logicalFactoryCalled = {false};
        ShardingDataSourceBootstrapper bootstrapper = new ShardingDataSourceBootstrapper(
                physicalFactory,
                loader,
                validator,
                migrator,
                (dataSources, yaml) -> {
                    logicalFactoryCalled[0] = true;
                    return mock(DataSource.class);
                });

        assertThatThrownBy(() -> bootstrapper.createDataSource(
                        ShardingTopologyValidatorTest.validProperties(),
                        new FlywayProperties()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("migration failed");

        assertThat(logicalFactoryCalled[0]).isFalse();
        verify(physicalFactory).close(physical.values());
    }
}
