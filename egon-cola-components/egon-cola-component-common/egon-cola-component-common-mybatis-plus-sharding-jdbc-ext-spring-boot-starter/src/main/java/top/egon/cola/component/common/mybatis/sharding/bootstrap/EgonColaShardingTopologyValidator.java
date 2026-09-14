package top.egon.cola.component.common.mybatis.sharding.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.broadcast.yaml.config.YamlBroadcastRuleConfiguration;
import org.apache.shardingsphere.driver.yaml.YamlJDBCConfiguration;
import org.apache.shardingsphere.infra.util.yaml.YamlEngine;
import org.apache.shardingsphere.readwritesplitting.yaml.config.YamlReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.sharding.yaml.config.YamlShardingRuleConfiguration;
import org.apache.shardingsphere.single.yaml.config.YamlSingleRuleConfiguration;
import org.apache.shardingsphere.transaction.yaml.config.YamlTransactionRuleConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.DataSourceRoleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.resolver.EgonColaShardingWriteTargetResolver;
import top.egon.cola.component.common.mybatis.sharding.strategy.EgonColaShardingStrategyNodes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Validates one typed ShardingSphere policy and materializes routing profiles.
 */
@Slf4j
@Component("egonColaShardingTopologyValidator")
@RequiredArgsConstructor
public class EgonColaShardingTopologyValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    public TopologyBO validate(EgonColaShardingProperties properties, byte[] yaml) {
        validationUtils.validate(properties);
        if (yaml == null || yaml.length == 0) {
            throw new EgonColaMybatisPlusConfigurationException("SHARDING_YAML_REQUIRED");
        }
        YamlJDBCConfiguration configuration = YamlEngine.unmarshal(
                new String(yaml, StandardCharsets.UTF_8), YamlJDBCConfiguration.class);
        if (!configuration.getDataSources().isEmpty()) {
            throw new EgonColaMybatisPlusConfigurationException("PHYSICAL_DATASOURCES_MUST_HAVE_ONE_OWNER");
        }
        if (configuration.getTransaction() == null) {
            configuration.setTransaction(new YamlTransactionRuleConfiguration());
        }
        String defaultType = configuration.getTransaction().getDefaultType();
        if (defaultType == null || defaultType.isBlank()) {
            configuration.getTransaction().setDefaultType(properties.getTransactionDefaultType());
            defaultType = properties.getTransactionDefaultType();
        }
        if (!"LOCAL".equals(defaultType) && !"XA".equals(defaultType)) {
            throw new EgonColaMybatisPlusConfigurationException("LOCAL_TRANSACTION_REQUIRED");
        }
        Map<String, String> schemas = new TreeMap<>();
        for (PhysicalDataSourceProperties source : properties.getDataSources()) {
            if (source.role() == DataSourceRoleEnum.PRIMARY) {
                schemas.putIfAbsent(source.logicalName(), "public");
            }
        }
        Map<String, EgonColaRoutingProfileBO> profiles = new TreeMap<>();
        Set<String> broadcast = new LinkedHashSet<>();
        if (configuration.getRules() != null) {
            for (Object rule : configuration.getRules()) {
                if (rule instanceof YamlSingleRuleConfiguration single) {
                    addSingle(single, schemas, profiles);
                } else if (rule instanceof YamlBroadcastRuleConfiguration copies) {
                    addBroadcast(copies, schemas, profiles, broadcast);
                } else if (rule instanceof YamlShardingRuleConfiguration sharding) {
                    addSharding(sharding, schemas, profiles);
                } else if (rule instanceof YamlReadwriteSplittingRuleConfiguration) {
                    continue;
                } else {
                    throw new EgonColaMybatisPlusConfigurationException("UNSUPPORTED_SHARDING_RULE");
                }
            }
        }
        if (profiles.isEmpty() && properties.getConfigStyle()
                == EgonColaShardingProperties.ConfigStyleEnum.STRATEGY
                && (properties.getTables() == null || properties.getTables().isEmpty())) {
            throw new EgonColaMybatisPlusConfigurationException("INVALID_TOPOLOGY");
        }
        String fingerprint = digest("ss-policy-v1|" + profiles + '|' + schemas);
        return new TopologyBO(YamlEngine.marshal(configuration).getBytes(StandardCharsets.UTF_8),
                profiles, schemas, broadcast, fingerprint);
    }

    private static void addSingle(YamlSingleRuleConfiguration single, Map<String, String> schemas,
                                  Map<String, EgonColaRoutingProfileBO> profiles) {
        if (single.getTables() == null) {
            return;
        }
        for (String table : single.getTables()) {
            String[] parts = table.split("\\.");
            if (parts.length != 3 || table.contains("*")) {
                throw new EgonColaMybatisPlusConfigurationException("SINGLE_NODE_REQUIRED");
            }
            EgonColaPhysicalTargetBO node = new EgonColaPhysicalTargetBO(parts[0], parts[1], parts[2]);
            put(profiles, EgonColaShardingStrategyNodes.simple(parts[2], TableKindEnum.SINGLE, List.of(node)));
        }
    }

    private static void addBroadcast(YamlBroadcastRuleConfiguration copies, Map<String, String> schemas,
                                     Map<String, EgonColaRoutingProfileBO> profiles, Set<String> broadcast) {
        if (copies.getTables() == null) {
            return;
        }
        for (String table : copies.getTables()) {
            List<EgonColaPhysicalTargetBO> nodes = new ArrayList<>();
            schemas.forEach((group, schema) -> nodes.add(new EgonColaPhysicalTargetBO(group, schema, table)));
            put(profiles, EgonColaShardingStrategyNodes.simple(table, TableKindEnum.BROADCAST_READ_ONLY, nodes));
            broadcast.add(table);
        }
    }

    private static void addSharding(YamlShardingRuleConfiguration sharding, Map<String, String> schemas,
                                    Map<String, EgonColaRoutingProfileBO> profiles) {
        if (sharding.getShardingAlgorithms() != null) {
            sharding.getShardingAlgorithms().values().forEach(algorithm -> {
                Properties normalized = new Properties();
                algorithm.getProps().forEach((key, value) -> normalized.setProperty(key.toString(), value.toString()));
                algorithm.setProps(normalized);
            });
        }
        if (sharding.getTables() == null) {
            return;
        }
        sharding.getTables().forEach((table, configured) -> {
            if (configured.getTableStrategy() != null && configured.getTableStrategy().getComplex() != null
                    && configured.getActualDataNodes() != null) {
                Properties props = new Properties();
                if (sharding.getShardingAlgorithms() != null
                        && configured.getTableStrategy().getComplex().getShardingAlgorithmName() != null) {
                    var algorithm = sharding.getShardingAlgorithms()
                            .get(configured.getTableStrategy().getComplex().getShardingAlgorithmName());
                    if (algorithm != null) {
                        props.putAll(algorithm.getProps());
                    }
                }
                props.setProperty("logical-table", table);
                props.setProperty("actual-data-nodes", configured.getActualDataNodes());
                put(profiles, EgonColaShardingWriteTargetResolver.profile(props));
            } else {
                List<EgonColaPhysicalTargetBO> nodes = new ArrayList<>();
                if (configured.getActualDataNodes() != null) {
                    for (String item : configured.getActualDataNodes().split(",")) {
                        String[] parts = item.trim().split("\\.");
                        if (parts.length == 3) {
                            nodes.add(new EgonColaPhysicalTargetBO(parts[0], parts[1], parts[2]));
                        } else if (parts.length == 2) {
                            nodes.add(new EgonColaPhysicalTargetBO(parts[0], schemas.getOrDefault(parts[0], "public"), parts[1]));
                        }
                    }
                }
                if (nodes.isEmpty()) {
                    throw new EgonColaMybatisPlusConfigurationException("INVALID_TOPOLOGY");
                }
                put(profiles, new EgonColaRoutingProfileBO(table, TableKindEnum.TENANT_LEGACY, "legacy-v1",
                        1, 1, Map.of(), null, null, EgonColaShardingStrategyNodes.SEED,
                        Map.of(new PartitionKeyBO(0, 0), nodes), 1, null));
            }
        });
    }

    private static void put(Map<String, EgonColaRoutingProfileBO> profiles, EgonColaRoutingProfileBO profile) {
        if (profiles.put(profile.logicalTable(), profile) != null) {
            throw new EgonColaMybatisPlusConfigurationException("LOGICAL_TABLE_RULE_OVERLAP");
        }
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA256_REQUIRED", failure);
        }
    }

    public record TopologyBO(byte[] yaml, Map<String, EgonColaRoutingProfileBO> profiles,
                             Map<String, String> schemas, Set<String> broadcastTables, String fingerprint) {
        public TopologyBO {
            yaml = yaml.clone();
            profiles = Map.copyOf(new TreeMap<>(profiles));
            schemas = Map.copyOf(schemas);
            broadcastTables = Set.copyOf(broadcastTables);
        }

        @Override
        public byte[] yaml() {
            return yaml.clone();
        }
    }
}
