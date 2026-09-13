package top.egon.cola.archetype.source.light.infrastructure.config.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.routing.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/** Adapter from the one validated SS YAML policy to the common write-target contract. */
@Slf4j
@RequiredArgsConstructor
public final class ShardingWriteTargetResolver implements EgonColaWriteTargetResolver {
    @Qualifier("egonColaRoutingProfiles")
    private final Map<String, EgonColaRoutingProfileBO> profiles;
    @Qualifier("shardingNodeMap")
    private final ShardingNodeMap legacy;
    @Qualifier("egonColaTwoLevelRouteStrategy")
    private final EgonColaTwoLevelRouteStrategy strategy;
    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;
    @Qualifier("shardingRouteFingerprint")
    private final String fingerprint;

    @Override
    public EgonColaRouteResult resolve(EgonColaRouteQuery query) {
        validation.validate(query);
        EgonColaRoutingProfileBO profile = profiles.get(query.logicalTable());
        if (profile == null) { throw new IllegalArgumentException("UNKNOWN_LOGICAL_TABLE"); }
        if (profile.kind() != EgonColaRoutingProfileBO.TableKindEnum.TENANT_LEGACY) {
            return strategy.route(profile, query);
        }
        ShardingNodeMap.PhysicalNode node = legacy.route(query.tenantId());
        List<EgonColaPhysicalTargetBO> targets = profile.actualNodes().values().stream().flatMap(Collection::stream)
                .filter(target -> target.group().equals(node.database()) && target.table().endsWith("_" + node.tableSuffix())).toList();
        if (targets.size() != 1) { throw new IllegalArgumentException("LEGACY_TARGET_UNRESOLVED"); }
        return new EgonColaRouteResult(targets, fingerprint);
    }

    static EgonColaRoutingProfileBO profile(Properties properties) {
        String table = required(properties, "logical-table");
        int tenantSlots = Integer.parseInt(required(properties, "tenant-slot-count"));
        int buckets = Integer.parseInt(required(properties, "secondary-bucket-count"));
        Map<Integer, String> slots = new LinkedHashMap<>();
        for (String entry : required(properties, "tenant-slot-map").split(",")) {
            String[] pair = entry.trim().split("=", -1);
            if (pair.length != 2 || slots.put(Integer.parseInt(pair[0]), pair[1]) != null) {
                throw new IllegalArgumentException("INVALID_TENANT_SLOT_MAP");
            }
        }
        Map<EgonColaRoutingProfileBO.PartitionKeyBO, List<EgonColaPhysicalTargetBO>> nodes = new LinkedHashMap<>();
        Pattern suffix = Pattern.compile(".*_t(\\d+)_b(\\d+)$");
        for (String item : required(properties, "actual-data-nodes").split(",")) {
            String[] parts = item.trim().split("\\.");
            if (parts.length != 3) { throw new IllegalArgumentException("EXPLICIT_GROUP_SCHEMA_TABLE_REQUIRED"); }
            var match = suffix.matcher(parts[2]);
            if (!match.matches()) { throw new IllegalArgumentException("TWO_LEVEL_TABLE_SUFFIX_REQUIRED"); }
            var key = new EgonColaRoutingProfileBO.PartitionKeyBO(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)));
            if (nodes.put(key, List.of(new EgonColaPhysicalTargetBO(parts[0], parts[1], parts[2]))) != null) {
                throw new IllegalArgumentException("DUPLICATE_PARTITION_NODE");
            }
        }
        String seed = required(properties, "secondary-seed");
        long parsedSeed = seed.startsWith("0x") ? Long.parseUnsignedLong(seed.substring(2), 16) : Long.parseUnsignedLong(seed);
        return new EgonColaRoutingProfileBO(table, EgonColaRoutingProfileBO.TableKindEnum.TENANT_ID_TWO_LEVEL,
                required(properties, "algorithm-version"), tenantSlots, buckets, slots,
                required(properties, "secondary-column"), required(properties, "root-key-name"), parsedSeed, nodes,
                Integer.parseInt(required(properties, "max-read-fanout-tables")), properties.getProperty("binding-group"));
    }

    private static String required(Properties properties, String key) {
        String value = properties == null ? null : properties.getProperty(key);
        if (value == null || value.isBlank()) { throw new IllegalArgumentException("Missing routing property: " + key); }
        return value;
    }
}
