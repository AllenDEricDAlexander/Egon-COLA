package top.egon.cola.component.common.mybatis.sharding;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ConfigStyleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.DataSourceRoleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ModeEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.TableProperties;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaBroadcastReadOnlyShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaComplexTenantThenBusinessShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaNativeYamlShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategyFactory;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaSingleTableShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaStandardTenantIdShardingStrategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EgonColaShardingStrategyFactoryTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejects_unknown_type() {
        assertThatThrownBy(() -> factory().create("HASH_MOD", table("SINGLE")))
                .isInstanceOf(EgonColaMybatisPlusConfigurationException.class)
                .hasMessageContaining("UNKNOWN_SHARDING_STRATEGY");
    }

    @Test
    void rejects_native_and_strategy_together() {
        EgonColaShardingProperties properties = EgonColaShardingProperties.builder()
                .mode(ModeEnum.SHARDING)
                .configStyle(ConfigStyleEnum.NATIVE)
                .nativeRulesResource("classpath:egon-ss-native.yml")
                .dataSources(List.of(primary("master_data")))
                .tables(Map.of("routing_metadata", table("SINGLE")))
                .build();
        assertThatThrownBy(() -> factory().create(properties))
                .isInstanceOf(EgonColaMybatisPlusConfigurationException.class)
                .hasMessageContaining("NATIVE_AND_STRATEGY");
    }

    @Test
    void creates_five_registered_types() {
        EgonColaShardingStrategyFactory factory = factory();
        for (String type : List.of(
                "SINGLE",
                "BROADCAST",
                "STANDARD_TENANT_ID",
                "COMPLEX_TENANT_THEN_BUSINESS",
                "NATIVE")) {
            assertThat(factory.create(type, table(type)).type()).isEqualTo(type);
        }
    }

    @Test
    void rejects_complex_without_root_key() {
        TableProperties table = new TableProperties(
                "COMPLEX_TENANT_THEN_BUSINESS",
                "shard_0",
                "tenant_id",
                List.of("tenant_id"),
                null);
        assertThatThrownBy(() -> factory().create("COMPLEX_TENANT_THEN_BUSINESS", table))
                .isInstanceOf(EgonColaMybatisPlusConfigurationException.class)
                .hasMessageContaining("SHARDING_KEY");
    }

    private static EgonColaShardingStrategyFactory factory() {
        ValidationUtils validationUtils = new ValidationUtils(VALIDATOR);
        Map<String, EgonColaShardingStrategy> strategies = new LinkedHashMap<>();
        register(strategies, new EgonColaSingleTableShardingStrategy());
        register(strategies, new EgonColaBroadcastReadOnlyShardingStrategy());
        register(strategies, new EgonColaStandardTenantIdShardingStrategy());
        register(strategies, new EgonColaComplexTenantThenBusinessShardingStrategy(
                new EgonColaTwoLevelRouteStrategy(validationUtils)));
        register(strategies, new EgonColaNativeYamlShardingStrategy());
        return new EgonColaShardingStrategyFactory(validationUtils, strategies);
    }

    private static void register(Map<String, EgonColaShardingStrategy> strategies, EgonColaShardingStrategy strategy) {
        strategies.put(strategy.type(), strategy);
    }

    private static TableProperties table(String type) {
        if ("COMPLEX_TENANT_THEN_BUSINESS".equals(type)) {
            return new TableProperties(type, "shard_0", "tenant_id", List.of("tenant_id", "order_id"), "order_id");
        }
        return new TableProperties(type, "master_data", "tenant_id", null, null);
    }

    private static PhysicalDataSourceProperties primary(String name) {
        return new PhysicalDataSourceProperties(
                name,
                name,
                DataSourceRoleEnum.PRIMARY,
                "org.postgresql.Driver",
                "jdbc:postgresql://localhost/" + name,
                "sa",
                "secret");
    }
}
