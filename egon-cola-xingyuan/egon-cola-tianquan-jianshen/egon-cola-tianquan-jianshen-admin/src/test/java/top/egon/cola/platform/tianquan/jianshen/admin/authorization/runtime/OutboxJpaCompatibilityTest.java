package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime;

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
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;
import top.egon.cola.component.outbox.persistence.dao.OutboxMessageDAO;
import top.egon.cola.component.outbox.store.MybatisPlusOutboxStore;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.PlatformTransactionManager;
import top.egon.cola.component.outbox.autoconfigure.OutboxInfrastructure;
import top.egon.cola.component.outbox.autoconfigure.OutboxInfrastructureResolver;
import top.egon.cola.component.outbox.autoconfigure.OutboxMetricsAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.OutboxMybatisPlusAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.OutboxStateMachineAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.common.exception.OutboxConfigurationException;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.withSettings;

class OutboxJpaCompatibilityTest {
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    @AfterAll static void closeValidators() { VALIDATORS.close(); }

    private ApplicationContextRunner mpFixture(DataSource selected) {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                EgonColaMybatisPlusAutoConfiguration.class, OutboxMybatisPlusAutoConfiguration.class,
                OutboxMetricsAutoConfiguration.class, TransactionalOutboxAutoConfiguration.class, OutboxStateMachineAutoConfiguration.class))
                .withUserConfiguration(MpFixtureConfiguration.class).withBean("dataSource", DataSource.class, () -> selected)
                .withBean(Validator.class, VALIDATORS::getValidator).withPropertyValues(
                        "egon.cola.component.mybatis-plus.tenant-id.ignored-tables[0]=egon_cola_outbox_message",
                        "egon.cola.component.transactional-outbox.storage.validate-schema=false",
                        "egon.cola.component.transactional-outbox.polling.enabled=false");
    }

    @Test
    void matchesJpaManagerAndMpFactory() throws Exception {
        DataSource selected = mock(DataSource.class);
        mpFixture(selected).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MybatisPlusOutboxStore.class);
            assertThat(context.getBean(OutboxInfrastructure.class).transactionManager()).isSameAs(context.getBean("transactionManager"));
            assertThat(((JpaTransactionManager) context.getBean("transactionManager")).getDataSource()).isSameAs(selected);
            assertThat(context.getBean(SqlSessionFactory.class).getConfiguration().getEnvironment().getDataSource()).isSameAs(selected);
        });
        verify(selected, never()).getConnection();
    }

    @Test
    void rejectsJpaManagerMismatchAndNullDatasource() throws Exception {
        for (String kind : List.of("OTHER", "NULL")) {
            DataSource selected = mock(DataSource.class);
            mpFixture(selected).withPropertyValues("test.outbox.tm-kind=" + kind).run(context -> {
                assertThat(context).hasFailed();
                assertThat(rootCause(context.getStartupFailure())).isInstanceOf(OutboxConfigurationException.class)
                        .hasMessageContaining("do not match");
            });
            verify(selected, never()).getConnection();
        }
    }

    @Test
    void rejectsUnsupportedManagersAndFactoryMismatch() throws Exception {
        for (String kind : List.of("UNKNOWN", "JTA")) {
            DataSource selected = mock(DataSource.class);
            mpFixture(selected).withPropertyValues("test.outbox.tm-kind=" + kind).run(context ->
                    assertThat(rootCause(context.getStartupFailure())).isInstanceOf(OutboxConfigurationException.class)
                            .hasMessageContaining("OUTBOX_DATASOURCE_TRANSACTION_MANAGER_REQUIRED"));
            verify(selected, never()).getConnection();
        }
        for (String key : List.of("factory-datasource-mismatch", "factory-transaction-mismatch")) {
            DataSource selected = mock(DataSource.class);
            mpFixture(selected).withPropertyValues("test.outbox." + key + "=true").run(context ->
                    assertThat(rootCause(context.getStartupFailure())).isInstanceOf(OutboxConfigurationException.class)
                            .hasMessageContaining("OUTBOX_SQL_SESSION_FACTORY_MISMATCH"));
            verify(selected, never()).getConnection();
        }
    }

    @Test
    void stillRejectsMultiplePrimariesAndMissingMapperXml() throws Exception {
        for (String key : List.of("multiple-primary", "mapper-xml-missing")) {
            DataSource selected = mock(DataSource.class);
            mpFixture(selected).withPropertyValues("test.outbox." + key + "=true").run(context -> {
                assertThat(context).hasFailed();
                assertThat(rootCause(context.getStartupFailure())).isInstanceOf(OutboxConfigurationException.class);
            });
            verify(selected, never()).getConnection();
        }
    }

    @Test
    void bootDefaultJpaOwnsSelectedOutboxManager() throws Exception {
        DataSource selected = mock(DataSource.class);
        var emf = mock(jakarta.persistence.EntityManagerFactory.class, withSettings().extraInterfaces(EntityManagerFactoryInfo.class));
        when(((EntityManagerFactoryInfo) emf).getDataSource()).thenReturn(selected);
        when(((EntityManagerFactoryInfo) emf).getJpaDialect()).thenReturn(new HibernateJpaDialect());
        bootOwnerFixture(selected, emf).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PlatformTransactionManager.class);
            var manager = context.getBean("transactionManager", JpaTransactionManager.class);
            assertThat(manager.getDataSource()).isSameAs(selected); assertThat(manager.getEntityManagerFactory()).isSameAs(emf);
            var definition = context.getBeanFactory().getBeanDefinition("transactionManager");
            assertThat(definition.getFactoryMethodName()).isEqualTo("transactionManager");
            assertThat(definition.getFactoryBeanName()).contains("HibernateJpaConfiguration");
            var properties = new TransactionalOutboxProperties(); properties.getStorage().setDataSourceBeanName("logicalDataSource");
            properties.getStorage().setTransactionManagerBeanName("transactionManager");
            assertThat(new OutboxInfrastructureResolver().resolve(context.getBeanFactory(), properties).transactionManager()).isSameAs(manager);
        });
        verify(selected, never()).getConnection();
    }

    @Test
    void competingNamedJdbcManagerBacksOffAndFailsConfiguredSelection() throws Exception {
        DataSource selected = mock(DataSource.class);
        var emf = mock(jakarta.persistence.EntityManagerFactory.class, withSettings().extraInterfaces(EntityManagerFactoryInfo.class));
        when(((EntityManagerFactoryInfo) emf).getDataSource()).thenReturn(selected);
        when(((EntityManagerFactoryInfo) emf).getJpaDialect()).thenReturn(new HibernateJpaDialect());
        bootOwnerFixture(selected, emf).withBean("legacyJdbcManager", DataSourceTransactionManager.class,
                () -> new DataSourceTransactionManager(selected)).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PlatformTransactionManager.class).doesNotHaveBean("transactionManager");
            var properties = new TransactionalOutboxProperties(); properties.getStorage().setDataSourceBeanName("logicalDataSource");
            properties.getStorage().setTransactionManagerBeanName("transactionManager");
            assertThatThrownBy(() -> new OutboxInfrastructureResolver().resolve(context.getBeanFactory(), properties))
                    .isInstanceOf(OutboxConfigurationException.class).hasMessageContaining("transactionManager");
        });
        verify(selected, never()).getConnection();
    }

    private ApplicationContextRunner bootOwnerFixture(DataSource selected, jakarta.persistence.EntityManagerFactory emf) {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
                org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
                org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration.class,
                org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizationAutoConfiguration.class))
                .withBean("logicalDataSource", DataSource.class, () -> selected)
                .withBean("entityManagerFactory", jakarta.persistence.EntityManagerFactory.class, () -> emf)
                .withPropertyValues("spring.jpa.database=POSTGRESQL", "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                        "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.open-in-view=false");
    }

    private static Throwable rootCause(Throwable failure) {
        assertThat(failure).isNotNull();
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) { current = current.getCause(); }
        return current;
    }

    @lombok.extern.slf4j.Slf4j
    @Configuration(value = "outboxJpaMpFixtureConfiguration", proxyBeanMethods = false)
    static class MpFixtureConfiguration {

        @Bean("transactionManager")
        PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource,
                org.springframework.core.env.Environment environment) {
            String kind = environment.getProperty("test.outbox.tm-kind", "JPA");
            if (kind.equals("UNKNOWN")) { return mock(PlatformTransactionManager.class); }
            if (kind.equals("JTA")) { return mock(org.springframework.transaction.jta.JtaTransactionManager.class); }
            JpaTransactionManager manager = mock(JpaTransactionManager.class);
            when(manager.getDataSource()).thenReturn(kind.equals("NULL") ? null : kind.equals("OTHER") ? mock(DataSource.class) : dataSource);
            return manager;
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
                @org.springframework.beans.factory.annotation.Value("${test.outbox.multiple-primary:false}") boolean multiplePrimary,
                @org.springframework.beans.factory.annotation.Value("${test.outbox.strategy-config-style:false}") boolean strategyConfigStyle
        ) {
            List<EgonColaShardingProperties.PhysicalDataSourceProperties> sources = multiplePrimary
                    ? List.of(primary("primary_a"), primary("primary_b"))
                    : List.of(primary("primary_a"));
            return EgonColaShardingProperties.builder()
                    .mode(EgonColaShardingProperties.ModeEnum.SHARDING)
                    .configStyle(strategyConfigStyle
                            ? EgonColaShardingProperties.ConfigStyleEnum.STRATEGY
                            : EgonColaShardingProperties.ConfigStyleEnum.NATIVE)
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
            boolean wrongTransactionFactory = environment.getProperty("test.outbox.factory-transaction-mismatch", Boolean.class, false);
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setEnvironment(new org.apache.ibatis.mapping.Environment("outbox-context-test",
                    wrongTransactionFactory ? new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory() : new org.mybatis.spring.transaction.SpringManagedTransactionFactory(),
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
