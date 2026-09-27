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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
        StringBuilder shardingTables = new StringBuilder();
        StringBuilder shardingAlgorithms = new StringBuilder();
        StringBuilder standaloneRules = new StringBuilder();
        Set<String> tableNames = new LinkedHashSet<>();
        Set<String> algorithmNames = new LinkedHashSet<>();
        yaml.append("databaseName: egon\nrules:\n");
        if (properties.getTables() != null) {
            for (Map.Entry<String, TableProperties> entry : properties.getTables().entrySet()) {
                EgonColaShardingStrategy strategy = strategyFactory.create(entry.getValue().type(), entry.getValue());
                String fragment = strategy.toRulesFragment(entry.getKey(), entry.getValue(), properties.getDataSources());
                if ("STANDARD_TENANT_ID".equals(strategy.type())) {
                    mergeStandardFragment(fragment, shardingTables, shardingAlgorithms, tableNames, algorithmNames);
                } else {
                    standaloneRules.append(fragment);
                }
            }
        }
        if (shardingTables.length() > 0) {
            yaml.append("- !SHARDING\n  tables:\n").append(shardingTables)
                    .append("  shardingAlgorithms:\n").append(shardingAlgorithms);
        }
        yaml.append(standaloneRules);
        yaml.append("transaction:\n  defaultType: ").append(properties.getTransactionDefaultType()).append('\n');
        return yaml.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void mergeStandardFragment(String fragment, StringBuilder tables, StringBuilder algorithms,
                                              Set<String> tableNames, Set<String> algorithmNames) {
        String tablesHeader = "tables:\n";
        String algorithmsHeader = "shardingAlgorithms:\n";
        if (fragment == null) {
            throw new EgonColaMybatisPlusConfigurationException("INVALID_STANDARD_SHARDING_FRAGMENT");
        }
        int algorithmsIndex = fragment.indexOf(algorithmsHeader);
        if (fragment == null || !fragment.startsWith(tablesHeader) || algorithmsIndex < tablesHeader.length()) {
            throw new EgonColaMybatisPlusConfigurationException("INVALID_STANDARD_SHARDING_FRAGMENT");
        }
        String tableSection = fragment.substring(tablesHeader.length(), algorithmsIndex);
        String algorithmSection = fragment.substring(algorithmsIndex + algorithmsHeader.length());
        appendUniqueEntries(tableSection, tables, tableNames);
        appendUniqueEntries(algorithmSection, algorithms, algorithmNames);
    }

    private static void appendUniqueEntries(String section, StringBuilder target, Set<String> names) {
        boolean hasEntry = false;
        for (String line : section.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            if (!line.startsWith("  ")) {
                throw new EgonColaMybatisPlusConfigurationException("INVALID_STANDARD_SHARDING_FRAGMENT");
            }
            if (!line.startsWith("    ")) {
                String entry = line.trim();
                if (!entry.endsWith(":") || !names.add(entry.substring(0, entry.length() - 1))) {
                    throw new EgonColaMybatisPlusConfigurationException("DUPLICATE_STANDARD_SHARDING_ENTRY");
                }
                hasEntry = true;
            }
        }
        if (!hasEntry) {
            throw new EgonColaMybatisPlusConfigurationException("INVALID_STANDARD_SHARDING_FRAGMENT");
        }
        section.lines().filter(line -> !line.isBlank()).forEach(line -> target.append("  ").append(line).append('\n'));
    }
}
