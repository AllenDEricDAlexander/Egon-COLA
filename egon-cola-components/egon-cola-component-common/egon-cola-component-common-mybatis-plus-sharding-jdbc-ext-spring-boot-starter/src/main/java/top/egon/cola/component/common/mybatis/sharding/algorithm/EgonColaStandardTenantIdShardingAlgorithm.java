package top.egon.cola.component.common.mybatis.sharding.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Routes one-level tenant sharding over the target names supplied by ShardingSphere. */
@Slf4j
public final class EgonColaStandardTenantIdShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    private volatile List<String> targetOrder = List.of();

    @Override
    public void init(Properties properties) {
        String configuredOrder = properties == null ? null : properties.getProperty("target-order");
        if (configuredOrder == null || configuredOrder.isBlank()) {
            throw new IllegalArgumentException("STANDARD_TARGET_ORDER_REQUIRED");
        }
        Set<String> orderedTargets = new LinkedHashSet<>();
        for (String candidate : configuredOrder.split(",", -1)) {
            String target = candidate.trim();
            if (target.isBlank() || !orderedTargets.add(target)) {
                throw new IllegalArgumentException("STANDARD_TARGET_ORDER_INVALID");
            }
        }
        targetOrder = List.copyOf(orderedTargets);
    }

    @Override
    public String doSharding(Collection<String> available, PreciseShardingValue<Long> value) {
        if (value == null || value.getValue() == null || value.getValue() <= 0 || available == null) {
            throw new IllegalArgumentException("SHARDING_KEY");
        }
        List<String> targets = targetOrder.stream().filter(available::contains).toList();
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("SHARDING_TARGET_UNAVAILABLE");
        }
        int index = (int) Long.remainderUnsigned(value.getValue(), targets.size());
        return targets.get(index);
    }

    @Override
    public Collection<String> doSharding(Collection<String> available, RangeShardingValue<Long> value) {
        throw new UnsupportedOperationException("TENANT_RANGE_FORBIDDEN");
    }
}
