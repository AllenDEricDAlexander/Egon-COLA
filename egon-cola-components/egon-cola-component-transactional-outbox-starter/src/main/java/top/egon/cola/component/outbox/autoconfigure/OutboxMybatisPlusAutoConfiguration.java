package top.egon.cola.component.outbox.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaShardingAutoConfiguration;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.outbox.migration.OutboxLogicalDataSourceFactory;
import top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer;

/** Registers the managed DDL hook before Common builds the logical datasource. */
@AutoConfiguration(after = EgonColaMybatisPlusAutoConfiguration.class, before = EgonColaShardingAutoConfiguration.class)
public class OutboxMybatisPlusAutoConfiguration {

    @Bean("outboxMpStorageProperties")
    @ConditionalOnMissingBean(name = "outboxMpStorageProperties")
    @ConfigurationProperties(prefix = OutboxMpStorageProperties.PREFIX, ignoreUnknownFields = false)
    public OutboxMpStorageProperties outboxMpStorageProperties() {
        return new OutboxMpStorageProperties();
    }

    @Bean("outboxManagedDdlInitializer")
    @ConditionalOnMissingBean(name = "outboxManagedDdlInitializer")
    public OutboxManagedDdlInitializer outboxManagedDdlInitializer(
            @Qualifier("egonColaPostgreDdlRunner") EgonColaPostgreDdlRunner ddlRunner,
            @Qualifier("egonColaShardingTopologyValidator") EgonColaShardingTopologyValidator topologyValidator,
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils,
            @Qualifier("egon.cola.component.mybatis-plus.sharding-top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties") EgonColaShardingProperties shardingProperties,
            @Qualifier("outboxMpStorageProperties") OutboxMpStorageProperties storageProperties,
            @Qualifier("outboxStateMachineObjectMapper") ObjectMapper objectMapper
    ) {
        return new OutboxManagedDdlInitializer(
                ddlRunner,
                topologyValidator,
                validationUtils,
                shardingProperties,
                storageProperties,
                objectMapper
        );
    }

    @Bean("egonColaShardingLogicalDataSourceFactory")
    @ConditionalOnMissingBean(name = "egonColaShardingLogicalDataSourceFactory")
    public LogicalDataSourceFactory egonColaShardingLogicalDataSourceFactory(
            @Qualifier("outboxManagedDdlInitializer") OutboxManagedDdlInitializer ddlInitializer
    ) {
        return new OutboxLogicalDataSourceFactory(ddlInitializer);
    }
}
