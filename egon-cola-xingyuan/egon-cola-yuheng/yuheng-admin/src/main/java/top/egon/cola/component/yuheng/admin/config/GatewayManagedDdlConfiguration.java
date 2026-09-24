package top.egon.cola.component.yuheng.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;

/**
 * 中文说明：占住组件唯一的逻辑数据源扩展点 {@code egonColaShardingLogicalDataSourceFactory}，让 Admin 在连接池建立后、
 * 逻辑数据源创建前完成受管 DDL。这里只允许<b>覆盖</b>组件既有扩展点，不允许另起一套 bootstrapper 或第二个连接池；
 * 两个开关（部署侧 {@code yuheng.persistence.managed-ddl-enabled} 与组件侧
 * {@code egon.cola.component.mybatis-plus.ddl.enabled}）必须一致，否则视为部署错误直接拒绝启动，
 * 因为只开一半会得到“以为建了库其实没建”的静默错配。
 * English summary: Occupies the component's single logical-datasource extension point
 * {@code egonColaShardingLogicalDataSourceFactory} so Admin completes managed DDL after the pools exist and before the
 * logical datasource is created. Only the existing extension point may be overridden — no second bootstrapper and no
 * second connection pool. The deployment switch ({@code yuheng.persistence.managed-ddl-enabled}) and the component
 * switch ({@code egon.cola.component.mybatis-plus.ddl.enabled}) must agree, otherwise startup is refused, because
 * enabling only one half yields the silent mismatch of "the schema was assumed to exist but never was created".
 *
 * 用法 / Usage: 由 Spring 容器装配；DDL 关闭时本配置整体退场，组件原厂
 * {@code YamlShardingSphereDataSourceFactory} 工厂保持默认行为不变。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class GatewayManagedDdlConfiguration {

    /**
     * 中文说明：注册 Admin 的受管 DDL 优先逻辑数据源工厂，bean 名即组件的扩展点名，故组件默认的
     * {@code @ConditionalOnMissingBean(name=...)} 工厂自动让位。
     * English summary: Registers Admin's managed-DDL-first logical datasource factory under the component's own
     * extension-point bean name, so the component's default {@code @ConditionalOnMissingBean(name=...)} factory steps aside.
     *
     * 用法 / Usage: 由容器调用一次，产出的工厂被组件 bootstrapper 在创建逻辑数据源时使用。
     * @return 受管 DDL 优先的逻辑数据源工厂；returns the managed-DDL-first logical datasource factory.
     */
    @Bean("egonColaShardingLogicalDataSourceFactory")
    @ConditionalOnProperty(prefix = "yuheng.persistence", name = "managed-ddl-enabled", havingValue = "true")
    public EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory egonColaShardingLogicalDataSourceFactory(
            EgonColaShardingProperties shardingProperties,
            EgonColaMybatisPlusProperties mybatisPlusProperties,
            ObjectMapper objectMapper,
            GatewayPersistenceProperties persistenceProperties,
            @Qualifier("egonColaShardingTopologyValidator") EgonColaShardingTopologyValidator topologyValidator,
            @Qualifier("egonColaPostgreDdlRunner") EgonColaPostgreDdlRunner ddlRunner) {
        if (!mybatisPlusProperties.getDdl().isEnabled()) {
            throw new EgonColaMybatisPlusConfigurationException("MANAGED_DDL_FLAG_CONFLICT");
        }
        log.info("managed ddl ownership claimed for expected schema version {}",
                persistenceProperties.getExpectedSchemaVersion());
        return new GatewayManagedDdlFactory(shardingProperties, objectMapper, persistenceProperties,
                topologyValidator, ddlRunner);
    }
}
