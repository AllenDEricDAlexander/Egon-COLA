package top.egon.cola.archetype.source.lightopen.infrastructure.config.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Physical pools → typed policy → managed DDL → readiness barrier → logical SS datasource. */
@Slf4j
@RequiredArgsConstructor
public final class ShardingDataSourceBootstrapper {
    @Qualifier("physicalDataSourceFactory")
    private final PhysicalDataSourceFactory physicalDataSourceFactory;
    @Qualifier("shardingYamlLoader")
    private final ShardingYamlLoader shardingYamlLoader;
    @Qualifier("shardingTopologyValidator")
    private final ShardingTopologyValidator topologyValidator;
    @Qualifier("egonColaPostgreDdlRunner")
    private final EgonColaPostgreDdlRunner ddlRunner;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties policy;
    @Qualifier("shardingLogicalDataSourceFactory")
    private final LogicalDataSourceFactory logicalDataSourceFactory;
    private volatile ShardingTopologyValidator.TopologyBO readyTopology;

    public DataSource createDataSource(ShardingDataSourceProperties properties) {
        Map<String, DataSource> physical = physicalDataSourceFactory.create(properties);
        try {
            var topology = topologyValidator.validate(properties, shardingYamlLoader.load(properties.config()));
            List<EgonColaDdlTargetBO> targets = targets(properties, physical, topology.fingerprint());
            if (policy.getDdl().isEnabled()) { ddlRunner.run(targets); }
            topologyValidator.verifyReadiness(topology, properties, physical, targets, policy.getDdl().getTopologyReadyTimeout());
            DataSource result = logicalDataSourceFactory.create(physical, topology.yaml());
            readyTopology = topology;
            return result;
        } catch (RuntimeException failure) {
            physicalDataSourceFactory.close(physical.values());
            throw failure;
        } catch (Exception failure) {
            physicalDataSourceFactory.close(physical.values());
            throw new IllegalStateException("SHARDING_LOGICAL_STARTUP_FAILED", failure);
        }
    }

    public Map<String, EgonColaRoutingProfileBO> profiles() {
        var topology = readyTopology;
        return topology == null ? Map.of() : topology.profiles();
    }

    public String fingerprint() {
        var topology = readyTopology;
        if (topology == null) { throw new IllegalStateException("SHARDING_TOPOLOGY_NOT_READY"); }
        return topology.fingerprint();
    }

    private List<EgonColaDdlTargetBO> targets(ShardingDataSourceProperties properties, Map<String, DataSource> physical,
                                            String fingerprint) throws java.io.IOException {
        List<EgonColaDdlTargetBO> result = new ArrayList<>();
        var resources = new PathMatchingResourcePatternResolver();
        for (var target : properties.ddl().targets()) {
            if (!target.manifest().startsWith("classpath:") || target.manifest().contains("..")) {
                throw new IllegalArgumentException("CLASSPATH_MANIFEST_REQUIRED");
            }
            var matches = resources.getResources("classpath*:" + target.manifest().substring("classpath:".length()));
            if (matches.length != 1) { throw new IllegalArgumentException("DDL_MANIFEST_RESOURCE_AMBIGUOUS"); }
            EgonColaDdlManifestBO manifest;
            try (var input = matches[0].getInputStream()) { manifest = objectMapper.readValue(input, EgonColaDdlManifestBO.class); }
            if (!"light-open".equals(manifest.family())) { throw new IllegalArgumentException("DDL_MANIFEST_FAMILY_MISMATCH"); }
            result.add(new EgonColaDdlTargetBO(target.dataSourceName(), target.schema(), target.role(),
                    physical.get(target.dataSourceName()), manifest, fingerprint));
        }
        return List.copyOf(result);
    }

    @FunctionalInterface
    interface LogicalDataSourceFactory {
        DataSource create(Map<String, DataSource> physical, byte[] yaml) throws Exception;
    }
}
