package top.egon.cola.component.common.mybatis.autoconfigure;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EgonColaShardingAutoConfigurationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void fails_without_sharding_yaml() {
        runner()
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().toString())
                            .contains("egon.cola.component.mybatis-plus.sharding");
                });
        runner()
                .withPropertyValues(
                        "egon.cola.component.mybatis-plus.sharding.enabled=false",
                        "egon.cola.component.mybatis-plus.sharding.mode=SHARDING",
                        "egon.cola.component.mybatis-plus.sharding.config-style=STRATEGY",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].name=master_data",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].logical-name=master_data",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].role=PRIMARY",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].driver-class-name=org.postgresql.Driver",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].jdbc-url=jdbc:postgresql://localhost/master_data",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].username=sa",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].password=secret")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasStackTraceContaining("SHARDING_REQUIRED"));
    }

    @Test
    void creates_primary_datasource_with_valid_yaml() {
        runner()
                .withUserConfiguration(MockLogicalDataSourceConfiguration.class)
                .withPropertyValues(
                        "egon.cola.component.mybatis-plus.sharding.enabled=true",
                        "egon.cola.component.mybatis-plus.sharding.mode=SHARDING",
                        "egon.cola.component.mybatis-plus.sharding.config-style=STRATEGY",
                        "egon.cola.component.mybatis-plus.sharding.transaction-default-type=LOCAL",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].name=master_data",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].logical-name=master_data",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].role=PRIMARY",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].driver-class-name=org.postgresql.Driver",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].jdbc-url=jdbc:postgresql://localhost/master_data",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].username=sa",
                        "egon.cola.component.mybatis-plus.sharding.data-sources[0].password=secret",
                        "egon.cola.component.mybatis-plus.sharding.tables.routing_metadata.type=SINGLE",
                        "egon.cola.component.mybatis-plus.sharding.tables.routing_metadata.data-source=master_data")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasBean("dataSource")
                        .hasBean("egonColaRoutingProfiles"));
    }

    private static ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withBean("snowflakeIdGenerator", LongIdGenerator.class, () -> () -> 1001L)
                .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
                .withConfiguration(AutoConfigurations.of(
                        EgonColaMybatisPlusAutoConfiguration.class,
                        EgonColaShardingAutoConfiguration.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class MockLogicalDataSourceConfiguration {
        @Bean("egonColaShardingLogicalDataSourceFactory")
        LogicalDataSourceFactory egonColaShardingLogicalDataSourceFactory() {
            return (physical, yaml) -> mock(DataSource.class);
        }
    }
}
