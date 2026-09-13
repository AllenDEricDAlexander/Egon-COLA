package top.egon.cola.archetype.source.service.infrastructure.config.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShardingDataSourceBootstrapperTest {
    @Test
    void createsLogicalDatasourceOnlyAfterDdlAndReadinessUsingTheSamePolicy() throws Exception {
        var properties = ShardingTopologyValidatorTest.validProperties();
        var yaml = ShardingTopologyValidatorTest.yaml(false);
        var topology = ShardingTopologyValidatorTest.validated(properties, yaml);
        var pools = mock(PhysicalDataSourceFactory.class);
        var loader = mock(ShardingYamlLoader.class);
        var validator = mock(ShardingTopologyValidator.class);
        var ddl = mock(EgonColaPostgreDdlRunner.class);
        var logical = mock(ShardingDataSourceBootstrapper.LogicalDataSourceFactory.class);
        Map<String, DataSource> physical = Map.of("master_data", mock(DataSource.class), "shard_0", mock(DataSource.class), "shard_1", mock(DataSource.class));
        when(pools.create(properties)).thenReturn(physical);
        when(loader.load(properties.config())).thenReturn(yaml);
        when(validator.validate(properties, yaml)).thenReturn(topology);
        DataSource expected = mock(DataSource.class);
        when(logical.create(eq(physical), any())).thenReturn(expected);
        var policy = new EgonColaMybatisPlusProperties();
        policy.getDdl().setEnabled(true);
        var bootstrapper = new ShardingDataSourceBootstrapper(pools, loader, validator, ddl, new ObjectMapper(), policy, logical);
        assertThat(bootstrapper.createDataSource(properties)).isSameAs(expected);
        var order = inOrder(validator, ddl, logical);
        order.verify(validator).validate(properties, yaml);
        order.verify(ddl).run(argThat(targets -> targets.size() == 3 && targets.stream().allMatch(target -> target.routeFingerprint().equals(topology.fingerprint()))));
        order.verify(validator).verifyReadiness(eq(topology), eq(properties), eq(physical), any(), eq(policy.getDdl().getTopologyReadyTimeout()));
        order.verify(logical).create(eq(physical), any());
        verify(pools, never()).close(any());
        assertThat(bootstrapper.profiles()).isSameAs(topology.profiles());
    }

    @Test
    void failedDdlClosesPoolsAndPreventsLogicalDatasourceCreation() throws Exception {
        var properties = ShardingTopologyValidatorTest.validProperties();
        var yaml = ShardingTopologyValidatorTest.yaml(false);
        var topology = ShardingTopologyValidatorTest.validated(properties, yaml);
        var pools = mock(PhysicalDataSourceFactory.class);
        var loader = mock(ShardingYamlLoader.class);
        var validator = mock(ShardingTopologyValidator.class);
        var ddl = mock(EgonColaPostgreDdlRunner.class);
        var logical = mock(ShardingDataSourceBootstrapper.LogicalDataSourceFactory.class);
        Map<String, DataSource> physical = Map.of("master_data", mock(DataSource.class), "shard_0", mock(DataSource.class), "shard_1", mock(DataSource.class));
        when(pools.create(properties)).thenReturn(physical);
        when(loader.load(properties.config())).thenReturn(yaml);
        when(validator.validate(properties, yaml)).thenReturn(topology);
        when(ddl.run(any())).thenThrow(new IllegalStateException("DDL_FAILED"));
        var policy = new EgonColaMybatisPlusProperties();
        policy.getDdl().setEnabled(true);
        var bootstrapper = new ShardingDataSourceBootstrapper(pools, loader, validator, ddl, new ObjectMapper(), policy, logical);
        assertThatThrownBy(() -> bootstrapper.createDataSource(properties)).hasMessage("DDL_FAILED");
        verify(pools).close(physical.values());
        verifyNoInteractions(logical);
        assertThat(bootstrapper.profiles()).isEmpty();
    }
}
