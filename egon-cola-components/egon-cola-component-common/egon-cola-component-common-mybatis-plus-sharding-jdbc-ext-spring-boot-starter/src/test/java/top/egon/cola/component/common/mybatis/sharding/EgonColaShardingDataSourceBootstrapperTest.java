package top.egon.cola.component.common.mybatis.sharding;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ConfigStyleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.DataSourceRoleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ModeEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaPhysicalDataSourceFactory;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingYamlLoader;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EgonColaShardingDataSourceBootstrapperTest {

    @Test
    void closes_pools_when_topology_invalid() throws Exception {
        EgonColaShardingProperties properties = properties();
        DataSource dataSource = mock(DataSource.class);
        Map<String, DataSource> physical = Map.of("shard_0", dataSource);
        EgonColaPhysicalDataSourceFactory pools = mock(EgonColaPhysicalDataSourceFactory.class);
        EgonColaShardingYamlLoader loader = mock(EgonColaShardingYamlLoader.class);
        EgonColaShardingTopologyValidator validator = mock(EgonColaShardingTopologyValidator.class);
        LogicalDataSourceFactory logical = mock(LogicalDataSourceFactory.class);
        when(pools.create(properties)).thenReturn(physical);
        when(loader.load(properties)).thenReturn(new byte[]{1});
        when(validator.validate(eq(properties), any())).thenThrow(
                new EgonColaMybatisPlusConfigurationException("INVALID_TOPOLOGY"));
        EgonColaShardingDataSourceBootstrapper bootstrapper =
                new EgonColaShardingDataSourceBootstrapper(pools, loader, validator, logical);
        assertThatThrownBy(() -> bootstrapper.createDataSource(properties))
                .isInstanceOf(EgonColaMybatisPlusConfigurationException.class)
                .hasMessageContaining("INVALID_TOPOLOGY");
        verify(pools).close(physical.values());
        verifyNoInteractions(logical);
    }

    private static EgonColaShardingProperties properties() {
        return EgonColaShardingProperties.builder()
                .mode(ModeEnum.SHARDING)
                .configStyle(ConfigStyleEnum.STRATEGY)
                .dataSources(List.of(new PhysicalDataSourceProperties(
                        "shard_0",
                        "shard_0",
                        DataSourceRoleEnum.PRIMARY,
                        "org.postgresql.Driver",
                        "jdbc:postgresql://localhost/shard_0",
                        "sa",
                        "secret")))
                .build();
    }
}
