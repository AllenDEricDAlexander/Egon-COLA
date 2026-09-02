package top.egon.cola.archetype.source.service.infrastructure.config.datasource;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Routes a positive tenant id to one stable database/table slot. */
public final class LongTenantShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    private ShardingNodeMap nodeMap;
    private Target target;

    @Override
    public void init(Properties properties) {
        nodeMap = ShardingNodeMap.parse(properties);
        String configuredTarget = properties.getProperty("target");
        if (configuredTarget == null || configuredTarget.isBlank()) {
            throw new IllegalArgumentException("sharding target must not be blank");
        }
        try {
            target = Target.valueOf(configuredTarget.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("sharding target must be database or table");
        }
    }

    @Override
    public String doSharding(
            Collection<String> availableTargetNames,
            PreciseShardingValue<Long> shardingValue) {
        if (nodeMap == null || target == null) {
            throw new IllegalStateException("sharding algorithm must be initialized");
        }
        Object rawTenantId = shardingValue == null
                ? null
                : ((PreciseShardingValue) shardingValue).getValue();
        if (!(rawTenantId instanceof Number number) || number.longValue() <= 0) {
            throw new IllegalArgumentException("tenant sharding value must be positive");
        }
        ShardingNodeMap.PhysicalNode node = nodeMap.route(number.longValue());
        return switch (target) {
            case DATABASE -> selectTarget(availableTargetNames,
                    available -> available.equals(node.database()));
            case TABLE -> selectTarget(availableTargetNames,
                    available -> available.endsWith("_" + node.tableSuffix()));
        };
    }

    @Override
    public Collection<String> doSharding(
            Collection<String> availableTargetNames,
            RangeShardingValue<Long> shardingValue) {
        throw new UnsupportedOperationException("range sharding is not supported");
    }

    private static String selectTarget(Collection<String> availableTargetNames,
                                       java.util.function.Predicate<String> selector) {
        if (availableTargetNames == null) {
            throw new IllegalArgumentException("available targets must not be null");
        }
        List<String> selected = availableTargetNames.stream().filter(selector).toList();
        if (selected.size() != 1) {
            throw new IllegalArgumentException("exactly one available target must match");
        }
        return selected.getFirst();
    }

    private enum Target {
        DATABASE,
        TABLE
    }
}
