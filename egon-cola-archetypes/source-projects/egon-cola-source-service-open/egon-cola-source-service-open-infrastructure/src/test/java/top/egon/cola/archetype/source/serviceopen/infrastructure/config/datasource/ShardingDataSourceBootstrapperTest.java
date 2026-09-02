package top.egon.cola.archetype.source.serviceopen.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
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

class ShardingDataSourceBootstrapperTest {

    @Test
    void shouldCreateLogicalDataSourceAfterValidationWithoutSchemaMutation() {
        PhysicalDataSourceFactory physicalFactory = mock(PhysicalDataSourceFactory.class);
        ShardingYamlLoader loader = mock(ShardingYamlLoader.class);
        ShardingTopologyValidator validator = mock(ShardingTopologyValidator.class);
        Map<String, DataSource> physical = new LinkedHashMap<>();
        physical.put("master_data", mock(DataSource.class));
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
                physicalFactory, loader, validator, logicalFactory);

        DataSource result = bootstrapper.createDataSource(
                ShardingTopologyValidatorTest.validProperties());

        assertThat(result).isSameAs(logical);
        InOrder order = inOrder(loader, validator);
        order.verify(loader).load("classpath:rules.yml");
        order.verify(validator).validate(any(), any());
        verify(physicalFactory, never()).close(any());
    }

    @Test
    void shouldCloseEveryPhysicalPoolWhenLogicalCreationFails() {
        PhysicalDataSourceFactory physicalFactory = mock(PhysicalDataSourceFactory.class);
        ShardingYamlLoader loader = mock(ShardingYamlLoader.class);
        ShardingTopologyValidator validator = mock(ShardingTopologyValidator.class);
        Map<String, DataSource> physical = Map.of("master_data", mock(DataSource.class));
        when(physicalFactory.create(any())).thenReturn(physical);
        when(loader.load(any())).thenReturn(new byte[0]);
        ShardingDataSourceBootstrapper bootstrapper = new ShardingDataSourceBootstrapper(
                physicalFactory, loader, validator,
                (dataSources, yaml) -> { throw new IllegalStateException("logical failed"); });

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                bootstrapper.createDataSource(ShardingTopologyValidatorTest.validProperties())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("logical failed");

        verify(physicalFactory).close(physical.values());
    }
}
