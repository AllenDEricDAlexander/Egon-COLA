package ${package}.infrastructure.config.datasource;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

/**
 * Routes a UUIDv7 key to the database or table represented by the same stable slot.
 */
public final class UuidV7BucketShardingAlgorithm implements StandardShardingAlgorithm<String> {

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
            PreciseShardingValue<String> shardingValue) {
        if (nodeMap == null || target == null) {
            throw new IllegalStateException("sharding algorithm must be initialized");
        }
        if (shardingValue == null) {
            throw new IllegalArgumentException("sharding value must not be null");
        }
        ShardingNodeMap.PhysicalNode node = nodeMap.route(shardingValue.getValue());
        return switch (target) {
            case DATABASE -> selectTarget(
                    availableTargetNames,
                    available -> available.equals(node.database()));
            case TABLE -> selectTarget(
                    availableTargetNames,
                    available -> available.endsWith("_" + node.tableSuffix()));
        };
    }

    @Override
    public Collection<String> doSharding(
            Collection<String> availableTargetNames,
            RangeShardingValue<String> shardingValue) {
        throw new UnsupportedOperationException("range sharding is not supported");
    }

    private static String selectTarget(
            Collection<String> availableTargetNames,
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
