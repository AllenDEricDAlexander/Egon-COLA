package top.egon.cola.component.outbox.autoconfigure;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.statemachine.StateMachine;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.persistence.repository.OutboxMessageRepository;
import top.egon.cola.component.outbox.persistence.OutboxTechnicalContextExecutor;
import top.egon.cola.component.outbox.store.MybatisPlusOutboxStore;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleService;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OutboxMybatisPlusAutoConfigurationTest {

    private static final String COMMON_MP_PROPERTIES_BEAN =
            "egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties";
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EgonColaMybatisPlusAutoConfiguration.class,
                    OutboxMybatisPlusAutoConfiguration.class,
                    OutboxMetricsAutoConfiguration.class,
                    TransactionalOutboxAutoConfiguration.class,
                    OutboxStateMachineAutoConfiguration.class))
            .withUserConfiguration(TestInfrastructureConfiguration.class)
            .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
            .withPropertyValues(
                    "egon.cola.component.mybatis-plus.tenant-id.ignored-tables[0]=egon_cola_outbox_message",
                    "egon.cola.component.transactional-outbox.storage.validate-schema=false",
                    "egon.cola.component.transactional-outbox.polling.enabled=false"
            );

    @Test
    void defaultRuntimeUsesManagedMpStoreAndTheSelectedFactory() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(OutboxStore.class)
                    .hasSingleBean(MybatisPlusOutboxStore.class)
                    .hasSingleBean(OutboxLifecycleService.class);
            assertThat(context).doesNotHaveBean("outboxJdbcTemplate")
                    .doesNotHaveBean("outboxNamedParameterJdbcTemplate");
            assertThat(context.getBean("outboxStore")).isSameAs(context.getBean(OutboxStore.class));
            assertThat(context.getBean("outboxStateMachineObjectMapper"))
                    .isSameAs(context.getBean(ObjectMapper.class));
            assertThat(context.getBean(DataSource.class))
                    .isSameAs(context.getBean(SqlSessionFactory.class).getConfiguration().getEnvironment().getDataSource());
            assertThat(context.getBean(SqlSessionFactory.class).getConfiguration()
                    .hasStatement("top.egon.cola.component.outbox.persistence.dao.OutboxMessageDAO.insertMessage", false))
                    .isTrue();
            assertThat(context.getBean(OutboxStateMachineProperties.class).getBusiness().isEnabled()).isFalse();
            assertThatThrownBy(() -> context.getBean(DeliveryHandlerRegistry.class).required("statemachine"))
                    .hasMessageContaining("No transactional outbox handler");
        });
    }

    @Test
    void generatedConstructorsRetainTheExactCommonMpPropertiesQualifier() {
        assertThat(propertiesQualifier(OutboxMessageRepository.class,
                EgonColaMybatisPlusProperties.class)).isEqualTo(COMMON_MP_PROPERTIES_BEAN);
        assertThat(propertiesQualifier(OutboxTechnicalContextExecutor.class,
                EgonColaMybatisPlusProperties.class)).isEqualTo(COMMON_MP_PROPERTIES_BEAN);
    }

    private static String propertiesQualifier(Class<?> type, Class<?> dependency) {
        Constructor<?> constructor = Arrays.stream(type.getConstructors())
                .filter(candidate -> Arrays.asList(candidate.getParameterTypes()).contains(dependency))
                .findFirst().orElseThrow();
        Parameter parameter = Arrays.stream(constructor.getParameters())
                .filter(candidate -> candidate.getType().equals(dependency))
                .findFirst().orElseThrow();
        Qualifier qualifier = parameter.getAnnotation(Qualifier.class);
        return qualifier == null ? null : qualifier.value();
    }

    @Test
    void mismatchedSessionFactoryDataSourceFailsWithoutJdbcFallback() {
        contextRunner.withPropertyValues("test.outbox.factory-datasource-mismatch=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().toString())
                            .contains("OUTBOX_SQL_SESSION_FACTORY_MISMATCH");
                });
    }

    @Test
    void multiplePrimaryDatasourcesFailClosed() {
        contextRunner.withPropertyValues("test.outbox.multiple-primary=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().toString())
                            .contains("OUTBOX_NATIVE_SINGLE_PRIMARY_REQUIRED");
                });
    }

    @Test
    void missingMapperXmlFailsInsteadOfCreatingAJdbcStore() {
        contextRunner.withPropertyValues("test.outbox.mapper-xml-missing=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().toString())
                            .contains("OUTBOX_MAPPER_XML_STATEMENT_REQUIRED");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestInfrastructureConfiguration {

        @Bean("dataSource")
        DataSource dataSource() {
            return mock(DataSource.class);
        }

        @Bean("transactionManager")
        DataSourceTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean("objectMapper")
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean("egonColaRoutingProfiles")
        Map<String, EgonColaRoutingProfileBO> routingProfiles() {
            return Map.of();
        }

        @Bean("egonColaWriteTargetResolver")
        top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver writeTargetResolver() {
            return query -> {
                throw new IllegalStateException("SHARDING_REQUIRED");
            };
        }

        @Bean("egonColaShardingRouteFingerprint")
        String routeFingerprint() {
            return "a".repeat(64);
        }

        @Bean("egon.cola.component.mybatis-plus.sharding-top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties")
        EgonColaShardingProperties shardingProperties(
                @org.springframework.beans.factory.annotation.Value("${test.outbox.multiple-primary:false}") boolean multiplePrimary
        ) {
            List<EgonColaShardingProperties.PhysicalDataSourceProperties> sources = multiplePrimary
                    ? List.of(primary("primary_a"), primary("primary_b"))
                    : List.of(primary("primary_a"));
            return EgonColaShardingProperties.builder()
                    .mode(EgonColaShardingProperties.ModeEnum.SHARDING)
                    .configStyle(EgonColaShardingProperties.ConfigStyleEnum.NATIVE)
                    .transactionDefaultType("LOCAL")
                    .dataSources(sources)
                    .tables(Map.of())
                    .build();
        }

        @Bean("egonColaShardingTopologyValidator")
        EgonColaShardingTopologyValidator shardingTopologyValidator() {
            return mock(EgonColaShardingTopologyValidator.class);
        }

        @Bean("sqlSessionFactory")
        SqlSessionFactory sqlSessionFactory(
                @Qualifier("dataSource") DataSource dataSource,
                @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
                EgonColaMybatisPlusProperties properties,
                org.springframework.core.env.Environment environment,
                MybatisPlusInterceptor outer,
                top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor validation,
                top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor original,
                top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator idGenerator,
                com.baomidou.mybatisplus.core.handlers.MetaObjectHandler metaObjectHandler
        ) throws IOException {
            boolean mismatch = environment.getProperty("test.outbox.factory-datasource-mismatch", Boolean.class, false);
            boolean missingXml = environment.getProperty("test.outbox.mapper-xml-missing", Boolean.class, false);
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setEnvironment(new org.apache.ibatis.mapping.Environment("outbox-context-test",
                    new org.mybatis.spring.transaction.SpringManagedTransactionFactory(),
                    mismatch ? mock(DataSource.class) : dataSource));
            configuration.addInterceptor(outer);
            configuration.addInterceptor(validation);
            configuration.addInterceptor(original);
            GlobalConfig global = new GlobalConfig();
            global.setDbConfig(new GlobalConfig.DbConfig());
            global.setMetaObjectHandler(metaObjectHandler);
            global.setIdentifierGenerator(idGenerator);
            GlobalConfigUtils.setGlobalConfig(configuration, global);
            configuration.addMapper(top.egon.cola.component.outbox.persistence.dao.OutboxMessageDAO.class);
            if (!missingXml) {
                try (InputStream mapperXml = new ClassPathResource("mapper/outbox/OutboxMessageMapper.xml").getInputStream()) {
                    new XMLMapperBuilder(mapperXml, configuration, "mapper/outbox/OutboxMessageMapper.xml",
                            configuration.getSqlFragments()).parse();
                }
            }
            return new DefaultSqlSessionFactory(configuration);
        }

        private static EgonColaShardingProperties.PhysicalDataSourceProperties primary(String name) {
            return new EgonColaShardingProperties.PhysicalDataSourceProperties(name, name,
                    EgonColaShardingProperties.DataSourceRoleEnum.PRIMARY, "org.postgresql.Driver",
                    "jdbc:postgresql://localhost/outbox_test", "test", "test");
        }
    }
}
