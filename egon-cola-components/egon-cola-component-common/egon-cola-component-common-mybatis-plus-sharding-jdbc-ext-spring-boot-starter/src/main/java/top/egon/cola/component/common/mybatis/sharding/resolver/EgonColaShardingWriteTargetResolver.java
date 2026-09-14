package top.egon.cola.component.common.mybatis.sharding.resolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteResult;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.routing.EgonColaWriteTargetResolver;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Adapter from the validated ShardingSphere policy to the common write-target contract.
 */
@Slf4j
@RequiredArgsConstructor
public class EgonColaShardingWriteTargetResolver implements EgonColaWriteTargetResolver {

    @Qualifier("egonColaRoutingProfiles")
    private final Map<String, EgonColaRoutingProfileBO> profiles;

    @Qualifier("egonColaTwoLevelRouteStrategy")
    private final EgonColaTwoLevelRouteStrategy strategy;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("egonColaShardingRouteFingerprint")
    private final String fingerprint;

    @Override
    public EgonColaRouteResult resolve(EgonColaRouteQuery query) {
        validationUtils.validate(query);
        EgonColaRoutingProfileBO profile = profiles.get(query.logicalTable());
        if (profile == null) {
            throw new EgonColaMybatisPlusConfigurationException("UNKNOWN_LOGICAL_TABLE");
        }
        if (profile.kind() != TableKindEnum.TENANT_LEGACY) {
            return strategy.route(profile, query);
        }
        List<EgonColaPhysicalTargetBO> all = profile.actualNodes().values().stream()
                .flatMap(Collection::stream)
                .toList();
        if (all.isEmpty()) {
            throw new EgonColaMybatisPlusConfigurationException("UNKNOWN_LOGICAL_TABLE");
        }
        int index = (int) Long.remainderUnsigned(query.tenantId(), all.size());
        return new EgonColaRouteResult(List.of(all.get(index)), fingerprint);
    }

    public static EgonColaRoutingProfileBO profile(Properties properties) {
        String table = required(properties, "logical-table");
        int tenantSlots = Integer.parseInt(required(properties, "tenant-slot-count"));
        int buckets = Integer.parseInt(required(properties, "secondary-bucket-count"));
        Map<Integer, String> slots = new LinkedHashMap<>();
        for (String entry : required(properties, "tenant-slot-map").split(",")) {
            String[] pair = entry.trim().split("=", -1);
            if (pair.length != 2 || slots.put(Integer.parseInt(pair[0]), pair[1]) != null) {
                throw new EgonColaMybatisPlusConfigurationException("INVALID_TOPOLOGY");
            }
        }
        Map<PartitionKeyBO, List<EgonColaPhysicalTargetBO>> nodes = new LinkedHashMap<>();
        Pattern suffix = Pattern.compile(".*_t(\\d+)_b(\\d+)$");
        for (String item : required(properties, "actual-data-nodes").split(",")) {
            String[] parts = item.trim().split("\\.");
            if (parts.length != 3) {
                throw new EgonColaMybatisPlusConfigurationException("INVALID_TOPOLOGY");
            }
            var match = suffix.matcher(parts[2]);
            if (!match.matches()) {
                throw new EgonColaMybatisPlusConfigurationException("INVALID_TOPOLOGY");
            }
            var key = new PartitionKeyBO(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)));
            if (nodes.put(key, List.of(new EgonColaPhysicalTargetBO(parts[0], parts[1], parts[2]))) != null) {
                throw new EgonColaMybatisPlusConfigurationException("INVALID_TOPOLOGY");
            }
        }
        String seed = required(properties, "secondary-seed");
        long parsedSeed = seed.startsWith("0x")
                ? Long.parseUnsignedLong(seed.substring(2), 16)
                : Long.parseUnsignedLong(seed);
        String binding = properties.getProperty("binding-group");
        return new EgonColaRoutingProfileBO(
                table,
                TableKindEnum.TENANT_ID_TWO_LEVEL,
                required(properties, "algorithm-version"),
                tenantSlots,
                buckets,
                slots,
                required(properties, "secondary-column"),
                required(properties, "root-key-name"),
                parsedSeed,
                nodes,
                Integer.parseInt(required(properties, "max-read-fanout-tables")),
                binding == null || binding.isBlank() ? null : binding);
    }

    private static String required(Properties properties, String key) {
        String value = properties == null ? null : properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new EgonColaMybatisPlusConfigurationException("INVALID_TOPOLOGY");
        }
        return value;
    }
}
