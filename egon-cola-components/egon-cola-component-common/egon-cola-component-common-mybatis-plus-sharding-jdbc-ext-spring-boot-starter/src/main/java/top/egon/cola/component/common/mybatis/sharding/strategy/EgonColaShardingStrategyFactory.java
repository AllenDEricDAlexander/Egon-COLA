package top.egon.cola.component.common.mybatis.sharding.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ConfigStyleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.TableProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry lookup for YAML table types. Unknown types and mixed NATIVE/STRATEGY fail fast.
 */
@Slf4j
@Component("egonColaShardingStrategyFactory")
@RequiredArgsConstructor
public class EgonColaShardingStrategyFactory {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("egonColaShardingStrategies")
    private final Map<String, EgonColaShardingStrategy> strategies;

    public EgonColaShardingStrategy create(String type) {
        if (type == null || type.isBlank()) {
            throw new EgonColaMybatisPlusConfigurationException("UNKNOWN_SHARDING_STRATEGY");
        }
        EgonColaShardingStrategy strategy = strategies.get(type);
        if (strategy == null) {
            log.error("unknown sharding strategy type={}", type);
            throw new EgonColaMybatisPlusConfigurationException("UNKNOWN_SHARDING_STRATEGY");
        }
        return strategy;
    }

    public EgonColaShardingStrategy create(String type, TableProperties table) {
        EgonColaShardingStrategy strategy = create(type);
        if (table != null) {
            validationUtils.validate(table);
            strategy.validateTable(table);
        }
        return strategy;
    }

    public Map<String, EgonColaShardingStrategy> create(EgonColaShardingProperties properties) {
        validationUtils.validate(properties);
        boolean hasTables = properties.getTables() != null && !properties.getTables().isEmpty();
        boolean hasNative = properties.getNativeRulesResource() != null
                && !properties.getNativeRulesResource().isBlank();
        if (properties.getConfigStyle() == ConfigStyleEnum.NATIVE && hasTables
                || properties.getConfigStyle() == ConfigStyleEnum.STRATEGY && hasNative
                || hasTables && hasNative) {
            throw new EgonColaMybatisPlusConfigurationException("NATIVE_AND_STRATEGY_CONFLICT");
        }
        if (properties.getConfigStyle() == ConfigStyleEnum.NATIVE) {
            return Map.of("NATIVE", create("NATIVE"));
        }
        Map<String, EgonColaShardingStrategy> selected = new LinkedHashMap<>();
        properties.getTables().forEach((logicalTable, table) ->
                selected.put(logicalTable, create(table.type(), table)));
        return selected;
    }
}
