package top.egon.cola.component.common.mybatis.autoconfigure;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.TableProperties;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategyFactory;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaStandardTenantIdShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingDataSourceBootstrapper.LogicalDataSourceFactory;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                .withPropertyValues(strategyProperties(
                        "tables.ddc_biz.type=STANDARD_TENANT_ID",
                        "tables.ddc_biz.sharding-column=tenant_id",
                        "tables.ddc_ai_resource.type=STANDARD_TENANT_ID",
                        "tables.ddc_ai_resource.sharding-column=tenant_id",
                        "tables.routing_metadata.type=SINGLE",
                        "tables.routing_metadata.data-source=master_data",
                        "tables.egon_cola_outbox_message.type=SINGLE",
                        "tables.egon_cola_outbox_message.data-source=master_data",
                        "tables.egon_cola_outbox_message.schema=egon_outbox",
                        "tables.ddl_history.type=SINGLE",
                        "tables.ddl_history.data-source=master_data",
                        "tables.ddl_history.schema=egon_outbox"))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasBean("dataSource").hasBean("egonColaRoutingProfiles");
                    Map<?, ?> profiles = context.getBean("egonColaRoutingProfiles", Map.class);
                    assertThat(routeSchema(profiles, "ddc_biz")).isEqualTo("public");
                    assertThat(routeSchema(profiles, "ddc_ai_resource")).isEqualTo("public");
                    assertThat(routeSchema(profiles, "routing_metadata")).isEqualTo("public");
                    assertThat(routeSchema(profiles, "egon_cola_outbox_message")).isEqualTo("egon_outbox");
                    assertThat(routeSchema(profiles, "ddl_history")).isEqualTo("egon_outbox");
                });
    }

    @Test
    void rejects_schema_override_for_non_single_strategy_tables() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "table.type", "STANDARD_TENANT_ID",
                "table.sharding-column", "tenant_id",
                "table.schema", "egon_outbox"));
        TableProperties table = new Binder(source).bind("table", Bindable.of(TableProperties.class))
                .orElseThrow(() -> new IllegalStateException("table route configuration is missing"));
        EgonColaShardingStrategyFactory factory = new EgonColaShardingStrategyFactory(
                new ValidationUtils(VALIDATOR_FACTORY.getValidator()),
                Map.of("STANDARD_TENANT_ID", new EgonColaStandardTenantIdShardingStrategy()));

        assertThatThrownBy(() -> factory.create("STANDARD_TENANT_ID", table))
                .hasMessageContaining("SCHEMA_OVERRIDE_REQUIRES_SINGLE");
    }

    @Test
    void standardTenantAlgorithmUsesOrderedModuloAndRejectsRanges() throws Exception {
        Class<?> algorithmType = Class.forName(
                "top.egon.cola.component.common.mybatis.sharding.algorithm.EgonColaStandardTenantIdShardingAlgorithm");
        assertThat(StandardShardingAlgorithm.class).isAssignableFrom(algorithmType);
        Object algorithm = algorithmType.getConstructor().newInstance();
        Properties properties = new Properties();
        properties.setProperty("target-order", "group_b,group_a");
        algorithmType.getMethod("init", Properties.class).invoke(algorithm, properties);
        PreciseShardingValue<Long> precise = mock(PreciseShardingValue.class);
        when(precise.getValue()).thenReturn(3L);
        var preciseMethod = algorithmType.getMethod("doSharding", Collection.class, PreciseShardingValue.class);
        assertThat(preciseMethod.invoke(algorithm, List.of("group_a", "group_b"), precise)).isEqualTo("group_a");

        RangeShardingValue<Long> range = mock(RangeShardingValue.class);
        var rangeMethod = algorithmType.getMethod("doSharding", Collection.class, RangeShardingValue.class);
        assertThatThrownBy(() -> rangeMethod.invoke(algorithm, List.of("group_a"), range))
                .hasRootCauseInstanceOf(UnsupportedOperationException.class)
                .hasRootCauseMessage("TENANT_RANGE_FORBIDDEN");
    }

    @Test
    void forcedShardingDataSourceBacksBootPoolConfigurationOff() {
        runner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
                .withUserConfiguration(MockLogicalDataSourceConfiguration.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://localhost/ordering",
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
                        .hasSingleBean(DataSource.class));
    }

    private static ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withBean(Validator.class, VALIDATOR_FACTORY::getValidator)
                .withConfiguration(AutoConfigurations.of(
                        EgonColaMybatisPlusAutoConfiguration.class,
                        EgonColaShardingAutoConfiguration.class));
    }

    private static String[] strategyProperties(String... tables) {
        String prefix = "egon.cola.component.mybatis-plus.sharding.";
        List<String> properties = new ArrayList<>(List.of(
                prefix + "enabled=true",
                prefix + "mode=SHARDING",
                prefix + "config-style=STRATEGY",
                prefix + "transaction-default-type=LOCAL",
                prefix + "data-sources[0].name=master_data",
                prefix + "data-sources[0].logical-name=master_data",
                prefix + "data-sources[0].role=PRIMARY",
                prefix + "data-sources[0].driver-class-name=org.postgresql.Driver",
                prefix + "data-sources[0].jdbc-url=jdbc:postgresql://localhost/master_data",
                prefix + "data-sources[0].username=sa",
                prefix + "data-sources[0].password=secret"));
        for (String table : tables) {
            properties.add(prefix + table);
        }
        return properties.toArray(String[]::new);
    }

    private static String routeSchema(Map<?, ?> profiles, String table) {
        EgonColaRoutingProfileBO profile = (EgonColaRoutingProfileBO) profiles.get(table);
        return profile.actualNodes().values().iterator().next().getFirst().schema();
    }

    @Configuration(proxyBeanMethods = false)
    static class MockLogicalDataSourceConfiguration {
        @Bean("egonColaShardingLogicalDataSourceFactory")
        LogicalDataSourceFactory egonColaShardingLogicalDataSourceFactory() {
            return (physical, yaml) -> mock(DataSource.class);
        }
    }
}
