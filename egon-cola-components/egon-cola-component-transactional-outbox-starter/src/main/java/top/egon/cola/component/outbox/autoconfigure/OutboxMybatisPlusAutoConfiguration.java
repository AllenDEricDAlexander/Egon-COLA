package top.egon.cola.component.outbox.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaShardingAutoConfiguration;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.outbox.migration.OutboxLogicalDataSourceFactory;
import top.egon.cola.component.outbox.migration.OutboxLegacyMigrationService;
import top.egon.cola.component.outbox.migration.OutboxManagedDdlInitializer;
import top.egon.cola.component.outbox.common.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.persistence.OutboxSchemaMetadataValidator;
import top.egon.cola.component.outbox.persistence.OutboxTechnicalContextExecutor;
import top.egon.cola.component.outbox.persistence.converter.OutboxHeadersConverter;
import top.egon.cola.component.outbox.persistence.converter.OutboxMessageConverter;
import top.egon.cola.component.outbox.persistence.converter.OutboxMessageConverterImpl;
import top.egon.cola.component.outbox.persistence.dao.OutboxMessageDAO;
import top.egon.cola.component.outbox.persistence.repository.OutboxMessageRepository;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleService;
import top.egon.cola.component.outbox.store.MybatisPlusOutboxStore;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Registers the managed DDL hook before Common builds the logical datasource. */
@AutoConfiguration(after = EgonColaMybatisPlusAutoConfiguration.class, before = EgonColaShardingAutoConfiguration.class)
@MapperScan(basePackageClasses = OutboxMessageDAO.class,
        sqlSessionFactoryRef = "${egon.cola.component.transactional-outbox.storage.mp.sql-session-factory-bean-name:sqlSessionFactory}")
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

    @Bean("outboxHeadersConverter")
    @ConditionalOnMissingBean(name = "outboxHeadersConverter")
    @ConditionalOnProperty(prefix = "egon.cola.component.transactional-outbox", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public OutboxHeadersConverter outboxHeadersConverter(
            @Qualifier("outboxStateMachineObjectMapper") ObjectMapper objectMapper,
            @Qualifier("outboxMessageValidator") OutboxMessageValidator messageValidator
    ) {
        return new OutboxHeadersConverter(objectMapper, messageValidator);
    }

    @Bean("outboxMessageConverterImpl")
    @ConditionalOnMissingBean(name = "outboxMessageConverterImpl")
    @ConditionalOnProperty(prefix = "egon.cola.component.transactional-outbox", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public OutboxMessageConverter outboxMessageConverterImpl(
            @Qualifier("outboxHeadersConverter") OutboxHeadersConverter headersConverter
    ) {
        return new OutboxMessageConverterImpl(headersConverter);
    }

    @Bean("outboxMessageRepository")
    @ConditionalOnMissingBean(name = "outboxMessageRepository")
    @ConditionalOnProperty(prefix = "egon.cola.component.transactional-outbox", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public OutboxMessageRepository outboxMessageRepository(
            @Qualifier("outboxMessageDAO") OutboxMessageDAO mapper,
            @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
            EgonColaMybatisPlusProperties properties
    ) {
        return new OutboxMessageRepository(mapper, properties);
    }

    @Bean("outboxTechnicalContextExecutor")
    @ConditionalOnMissingBean(name = "outboxTechnicalContextExecutor")
    @ConditionalOnProperty(prefix = "egon.cola.component.transactional-outbox", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public OutboxTechnicalContextExecutor outboxTechnicalContextExecutor(
            @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
            EgonColaMybatisPlusProperties properties
    ) {
        return new OutboxTechnicalContextExecutor(properties);
    }

    @Bean("outboxSchemaMetadataValidator")
    @ConditionalOnMissingBean(name = "outboxSchemaMetadataValidator")
    @ConditionalOnProperty(prefix = "egon.cola.component.transactional-outbox", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public OutboxSchemaMetadataValidator outboxSchemaMetadataValidator(
            @Qualifier("outboxManagedDdlInitializer") OutboxManagedDdlInitializer ddlInitializer,
            @Qualifier("egonColaShardingRouteFingerprint") String routeFingerprint,
            @Qualifier("outboxMpStorageProperties") OutboxMpStorageProperties storageProperties,
            @Qualifier("outboxStateMachineObjectMapper") ObjectMapper objectMapper,
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils
    ) {
        return new OutboxSchemaMetadataValidator(
                ddlInitializer, routeFingerprint, storageProperties, objectMapper, validationUtils);
    }

    @Bean("outboxWorkerTransactionTemplate")
    @ConditionalOnMissingBean(name = "outboxWorkerTransactionTemplate")
    @ConditionalOnProperty(prefix = "egon.cola.component.transactional-outbox", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public TransactionTemplate outboxWorkerTransactionTemplate(
            @Qualifier("outboxInfrastructure") OutboxInfrastructure infrastructure
    ) {
        TransactionTemplate transaction = new TransactionTemplate(infrastructure.transactionManager());
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction;
    }

    @Bean("outboxStore")
    @ConditionalOnMissingBean(OutboxStore.class)
    @ConditionalOnProperty(prefix = "egon.cola.component.transactional-outbox", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public OutboxStore outboxStore(
            @Qualifier("outboxMessageRepository") OutboxMessageRepository repository,
            @Qualifier("outboxMessageConverterImpl") OutboxMessageConverter converter,
            @Qualifier("outboxTechnicalContextExecutor") OutboxTechnicalContextExecutor technicalContext,
            @Qualifier("outboxLifecycleService") OutboxLifecycleService lifecycle,
            @Qualifier("outboxWorkerTransactionTemplate") TransactionTemplate workerTransaction,
            @Qualifier("outboxSchemaMetadataValidator") OutboxSchemaMetadataValidator metadataValidator,
            @Qualifier("egonColaMybatisPlusClock") java.time.Clock clock,
            @Qualifier("outboxInfrastructure") OutboxInfrastructure infrastructure,
            @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
            EgonColaMybatisPlusProperties mybatisProperties,
            @Qualifier("egon.cola.component.mybatis-plus.sharding-top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties")
            EgonColaShardingProperties shardingProperties,
            @Qualifier("outboxMpStorageProperties") OutboxMpStorageProperties storageProperties,
            ConfigurableListableBeanFactory beanFactory
    ) {
        validateStoreInfrastructure(infrastructure, mybatisProperties, shardingProperties,
                storageProperties, beanFactory);
        return new MybatisPlusOutboxStore(repository, converter, technicalContext, lifecycle,
                workerTransaction, metadataValidator, clock, storageProperties);
    }

    @Bean("outboxLegacyMigrationService")
    @ConditionalOnMissingBean(name = "outboxLegacyMigrationService")
    @ConditionalOnProperty(prefix = "egon.cola.component.transactional-outbox", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public OutboxLegacyMigrationService outboxLegacyMigrationService(
            @Qualifier("outboxManagedDdlInitializer") OutboxManagedDdlInitializer ddlInitializer,
            @Qualifier("egonColaShardingRouteFingerprint") String routeFingerprint,
            @Qualifier("outboxMessageRepository") OutboxMessageRepository repository,
            @Qualifier("outboxMessageConverterImpl") OutboxMessageConverter converter,
            @Qualifier("outboxHeadersConverter") OutboxHeadersConverter headersConverter,
            @Qualifier("outboxMessageValidator") OutboxMessageValidator messageValidator,
            @Qualifier("outboxTechnicalContextExecutor") OutboxTechnicalContextExecutor technicalContext,
            @Qualifier("outboxMpStorageProperties") OutboxMpStorageProperties storageProperties,
            @Qualifier("outboxWorkerTransactionTemplate") TransactionTemplate workerTransaction,
            @Qualifier("egonColaValidationUtils") ValidationUtils validationUtils
    ) {
        return new OutboxLegacyMigrationService(
                ddlInitializer, routeFingerprint, repository, converter, headersConverter, messageValidator,
                technicalContext, storageProperties, workerTransaction, validationUtils);
    }

    private static void validateStoreInfrastructure(
            OutboxInfrastructure infrastructure,
            EgonColaMybatisPlusProperties mybatisProperties,
            EgonColaShardingProperties shardingProperties,
            OutboxMpStorageProperties storageProperties,
            ConfigurableListableBeanFactory beanFactory
    ) {
        if (shardingProperties.getConfigStyle() != EgonColaShardingProperties.ConfigStyleEnum.NATIVE
                || shardingProperties.getMode() != EgonColaShardingProperties.ModeEnum.SHARDING
                || shardingProperties.getDataSources() == null || shardingProperties.getDataSources().size() != 1
                || shardingProperties.getDataSources().getFirst().role()
                != EgonColaShardingProperties.DataSourceRoleEnum.PRIMARY) {
            throw new OutboxConfigurationException("OUTBOX_NATIVE_SINGLE_PRIMARY_REQUIRED");
        }
        if (!mybatisProperties.getTenantId().ignores("egon_cola_outbox_message")) {
            throw new OutboxConfigurationException("OUTBOX_TENANT_TABLE_IGNORE_REQUIRED");
        }
        if (!(infrastructure.transactionManager() instanceof DataSourceTransactionManager transactionManager)
                || transactionManager.getDataSource() != infrastructure.dataSource()) {
            throw new OutboxConfigurationException("OUTBOX_DATASOURCE_TRANSACTION_MANAGER_REQUIRED");
        }

        String factoryName = storageProperties.getSqlSessionFactoryBeanName();
        Class<?> factoryType = beanFactory.getType(factoryName);
        if (!beanFactory.containsBean(factoryName) || factoryType == null
                || !SqlSessionFactory.class.isAssignableFrom(factoryType)) {
            throw new OutboxConfigurationException("OUTBOX_SQL_SESSION_FACTORY_REQUIRED: " + factoryName);
        }
        SqlSessionFactory factory = beanFactory.getBean(factoryName, SqlSessionFactory.class);
        var environment = factory.getConfiguration().getEnvironment();
        if (environment == null || environment.getDataSource() != infrastructure.dataSource()
                || !(environment.getTransactionFactory() instanceof SpringManagedTransactionFactory)) {
            throw new OutboxConfigurationException("OUTBOX_SQL_SESSION_FACTORY_MISMATCH: " + factoryName);
        }
        if (!factory.getConfiguration().getMapperRegistry().hasMapper(OutboxMessageDAO.class)) {
            throw new OutboxConfigurationException("OUTBOX_MAPPER_SCAN_REQUIRED");
        }
        Set<String> methodNames = new LinkedHashSet<>();
        for (Method method : OutboxMessageDAO.class.getDeclaredMethods()) {
            if (!method.isDefault()) {
                methodNames.add(method.getName());
            }
        }
        for (Method method : top.egon.cola.component.common.mybatis.extension.EgonColaMapper.class.getDeclaredMethods()) {
            try {
                Method implementation = OutboxMessageDAO.class.getMethod(method.getName(), method.getParameterTypes());
                if (!implementation.isDefault()) {
                    methodNames.add(method.getName());
                }
            } catch (NoSuchMethodException failure) {
                throw new OutboxConfigurationException("OUTBOX_MAPPER_METHOD_CONTRACT_MISSING: " + method.getName(), failure);
            }
        }
        for (String method : methodNames) {
            String statement = OutboxMessageDAO.class.getName() + '.' + method;
            var mapped = factory.getConfiguration().hasStatement(statement, false)
                    ? factory.getConfiguration().getMappedStatement(statement, false) : null;
            if (mapped == null || mapped.getResource() == null || !mapped.getResource().contains(".xml")) {
                throw new OutboxConfigurationException("OUTBOX_MAPPER_XML_STATEMENT_REQUIRED: " + method);
            }
        }
    }
}
