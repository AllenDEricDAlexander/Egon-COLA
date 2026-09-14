package top.egon.cola.component.common.mybatis.sharding.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator.TopologyBO;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Physical pools → typed policy → logical ShardingSphere DataSource. Closes pools on any failure.
 */
@Slf4j
@RequiredArgsConstructor
public class EgonColaShardingDataSourceBootstrapper {

    @Qualifier("egonColaPhysicalDataSourceFactory")
    private final EgonColaPhysicalDataSourceFactory physicalDataSourceFactory;

    @Qualifier("egonColaShardingYamlLoader")
    private final EgonColaShardingYamlLoader yamlLoader;

    @Qualifier("egonColaShardingTopologyValidator")
    private final EgonColaShardingTopologyValidator topologyValidator;

    @Qualifier("egonColaShardingLogicalDataSourceFactory")
    private final LogicalDataSourceFactory logicalDataSourceFactory;

    private volatile TopologyBO readyTopology;

    public DataSource createDataSource(EgonColaShardingProperties properties) {
        Map<String, DataSource> physical = physicalDataSourceFactory.create(properties);
        try {
            byte[] yaml = yamlLoader.load(properties);
            TopologyBO topology = topologyValidator.validate(properties, yaml);
            DataSource logical = logicalDataSourceFactory.create(physical, topology.yaml());
            readyTopology = topology;
            return logical;
        } catch (RuntimeException failure) {
            physicalDataSourceFactory.close(physical.values());
            throw failure;
        } catch (Exception failure) {
            physicalDataSourceFactory.close(physical.values());
            throw new EgonColaMybatisPlusConfigurationException("SHARDING_LOGICAL_STARTUP_FAILED", failure);
        }
    }

    public Map<String, EgonColaRoutingProfileBO> profiles() {
        TopologyBO topology = readyTopology;
        return topology == null ? Map.of() : topology.profiles();
    }

    public String fingerprint() {
        TopologyBO topology = readyTopology;
        if (topology == null) {
            throw new EgonColaMybatisPlusConfigurationException("SHARDING_TOPOLOGY_NOT_READY");
        }
        return topology.fingerprint();
    }

    @FunctionalInterface
    public interface LogicalDataSourceFactory {
        DataSource create(Map<String, DataSource> physical, byte[] yaml) throws Exception;
    }
}
