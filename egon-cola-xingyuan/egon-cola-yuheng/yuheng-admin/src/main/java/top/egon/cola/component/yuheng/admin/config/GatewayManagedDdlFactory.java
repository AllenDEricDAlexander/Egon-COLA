package top.egon.cola.component.yuheng.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlResult;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;

/**
 * 中文说明：{@code GatewayManagedDdlConfiguration} 提供的组件扩展点实现：在组件已经建好物理连接池并校验过 YAML 之后、
 * 逻辑数据源创建<b>之前</b>，先把受管 DDL 跑完，只有全部目标成功才把创建逻辑数据源的工作交回组件原厂的
 * {@link YamlShardingSphereDataSourceFactory}。拓扑不重新发明：再次调用组件 {@link EgonColaShardingTopologyValidator}
 * 得到同一份 profiles/schema/fingerprint，因此绝不自行拼接 hash 算法，也不复制 bootstrapper。
 * English summary: The component extension point contributed by {@code GatewayManagedDdlConfiguration}: once the component
 * has built the physical pools and validated the YAML, and <b>before</b> the logical datasource exists, the managed DDL is
 * executed to completion and only then is logical-datasource creation handed back to the component's own
 * {@link YamlShardingSphereDataSourceFactory}. The topology is never re-derived locally — the component
 * {@link EgonColaShardingTopologyValidator} is invoked again to obtain the same profiles/schema/fingerprint, so no
 * fingerprint hashing algorithm is reinvented and no bootstrapper copy is made.
 *
 * 用法 / Usage: 仅由 {@code GatewayManagedDdlConfiguration} 以 {@code egonColaShardingLogicalDataSourceFactory} 名义装配；
 * 业务代码不得直接调用。失败一律向上抛出以 fail-closed 启动，空库外的未知 schema 由 runner 拒绝而不是本机修复。
 */
@Slf4j
@RequiredArgsConstructor
public final class GatewayManagedDdlFactory implements EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory {

    private static final String MANIFEST_RESOURCE = "db/egon-mp/repository-manifest.json";

    private final EgonColaShardingProperties shardingProperties;
    private final ObjectMapper objectMapper;
    private final GatewayPersistenceProperties persistenceProperties;
    @Qualifier("egonColaShardingTopologyValidator")
    private final EgonColaShardingTopologyValidator topologyValidator;
    @Qualifier("egonColaPostgreDdlRunner")
    private final EgonColaPostgreDdlRunner ddlRunner;

    @Override
    public DataSource create(Map<String, DataSource> physical, byte[] yaml) throws Exception {
        // 期望版本/摘要先于拓扑与任何连接动作校验：配置错配是部署错误，不该表现为一次半途而废的 DDL 尝试。
        // The expected version/checksum is checked before the topology and any connection work: a mis-deployment is a
        // configuration error and must not surface as a half-executed DDL attempt.
        EgonColaDdlManifestBO manifest = readManifest();
        requireExpectedScript(manifest);
        var topology = topologyValidator.validate(shardingProperties, yaml);
        ddlRunner.run(targets(physical, topology.fingerprint(), topology.schemas(), manifest)).stream()
                .filter(result -> EgonColaDdlResult.StatusEnum.APPLIED.equals(result.status()))
                .forEach(result -> log.info("managed ddl applied alias={} schema={} version={}",
                        result.alias(), result.schema(), result.version()));
        return YamlShardingSphereDataSourceFactory.createDataSource(physical, topology.yaml());
    }

    private EgonColaDdlManifestBO readManifest() {
        try (InputStream manifestStream = getClass().getClassLoader().getResourceAsStream(MANIFEST_RESOURCE)) {
            if (manifestStream == null) {
                throw new EgonColaMybatisPlusConfigurationException("DDL_MANIFEST_MISSING");
            }
            return objectMapper.readValue(manifestStream, EgonColaDdlManifestBO.class);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private void requireExpectedScript(EgonColaDdlManifestBO manifest) {
        EgonColaDdlManifestBO.ScriptBO head = manifest.scripts().get(manifest.scripts().size() - 1);
        if (!head.version().equals(persistenceProperties.getExpectedSchemaVersion())
                || !head.sha256().equals(persistenceProperties.getExpectedSchemaSha256())) {
            throw new EgonColaMybatisPlusConfigurationException("SCHEMA_MANIFEST_MISMATCH");
        }
    }

    private List<EgonColaDdlTargetBO> targets(Map<String, DataSource> physical, String routeFingerprint,
                                              Map<String, String> schemas, EgonColaDdlManifestBO manifest) {
        List<EgonColaDdlTargetBO> targets = new ArrayList<>();
        for (EgonColaShardingProperties.PhysicalDataSourceProperties dataSource : shardingProperties.getDataSources()) {
            if (!EgonColaShardingProperties.DataSourceRoleEnum.PRIMARY.equals(dataSource.role())) {
                continue;
            }
            DataSource physicalDataSource = physical.get(dataSource.name());
            String schema = schemas.get(dataSource.logicalName());
            if (physicalDataSource == null || schema == null) {
                throw new EgonColaMybatisPlusConfigurationException("DDL_PRIMARY_TARGET_UNRESOLVED");
            }
            targets.add(new EgonColaDdlTargetBO(dataSource.name(), schema,
                    EgonColaDdlTargetBO.RoleEnum.SHARD, physicalDataSource, manifest, routeFingerprint));
        }
        if (targets.isEmpty()) {
            throw new EgonColaMybatisPlusConfigurationException("DDL_PRIMARY_TARGET_UNRESOLVED");
        }
        return List.copyOf(targets);
    }
}
