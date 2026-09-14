package top.egon.cola.component.common.mybatis.sharding.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ConfigStyleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.TableProperties;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategy;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategyFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads native ShardingSphere YAML or assembles STRATEGY fragments into one rules document.
 */
@Slf4j
@Component("egonColaShardingYamlLoader")
@RequiredArgsConstructor
public class EgonColaShardingYamlLoader {

    @Qualifier("egonColaShardingStrategyFactory")
    private final EgonColaShardingStrategyFactory strategyFactory;

    private final ResourceLoader resourceLoader;

    private final Environment environment;

    public byte[] load(EgonColaShardingProperties properties) {
        if (properties.getConfigStyle() == ConfigStyleEnum.NATIVE) {
            return loadNative(properties.getNativeRulesResource());
        }
        return assembleStrategy(properties);
    }

    private byte[] loadNative(String location) {
        if (location == null || location.isBlank()) {
            throw new EgonColaMybatisPlusConfigurationException("NATIVE_YAML_REQUIRED");
        }
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new EgonColaMybatisPlusConfigurationException("NATIVE_YAML_REQUIRED");
        }
        try (var inputStream = resource.getInputStream()) {
            String yaml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return environment.resolveRequiredPlaceholders(yaml).getBytes(StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new EgonColaMybatisPlusConfigurationException("NATIVE_YAML_REQUIRED", failure);
        }
    }

    private byte[] assembleStrategy(EgonColaShardingProperties properties) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("databaseName: egon\nrules:\n");
        if (properties.getTables() != null) {
            for (Map.Entry<String, TableProperties> entry : properties.getTables().entrySet()) {
                EgonColaShardingStrategy strategy = strategyFactory.create(entry.getValue().type(), entry.getValue());
                yaml.append(strategy.toRulesFragment(entry.getKey(), entry.getValue(), properties.getDataSources()));
            }
        }
        yaml.append("transaction:\n  defaultType: ").append(properties.getTransactionDefaultType()).append('\n');
        return yaml.toString().getBytes(StandardCharsets.UTF_8);
    }
}
