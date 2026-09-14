package top.egon.cola.component.common.mybatis.sharding.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.sharding.resolver.EgonColaShardingWriteTargetResolver;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Table candidates for tenant then business-root two-level sharding.
 */
@Slf4j
public final class EgonColaTenantBusinessComplexShardingAlgorithm implements ComplexKeysShardingAlgorithm<Long> {

    private EgonColaRoutingProfileBO profile;

    @Override
    public void init(Properties properties) {
        profile = EgonColaShardingWriteTargetResolver.profile(properties);
    }

    @Override
    public Collection<String> doSharding(Collection<String> available, ComplexKeysShardingValue<Long> value) {
        if (profile == null || value == null || value.getColumnNameAndShardingValuesMap() == null
                || value.getColumnNameAndRangeValuesMap().containsKey("tenant_id")) {
            throw new IllegalArgumentException("SHARDING_KEY");
        }
        Collection<Long> tenants = value.getColumnNameAndShardingValuesMap().get("tenant_id");
        if (tenants == null || tenants.size() != 1) {
            throw new IllegalArgumentException("SHARDING_KEY");
        }
        Collection<Long> selected = value.getColumnNameAndShardingValuesMap().get(profile.secondaryColumn());
        List<Long> roots = selected == null ? List.of() : List.copyOf(selected);
        boolean range = roots.isEmpty() && value.getColumnNameAndRangeValuesMap().containsKey(profile.secondaryColumn());
        var result = EgonColaTwoLevelRouteStrategy.routeCandidates(profile, new EgonColaRouteQuery(
                value.getLogicTableName(),
                tenants.iterator().next(),
                roots,
                EgonColaRouteQuery.OperationEnum.ROUTE_CANDIDATES,
                range));
        List<String> tables = result.targets().stream().map(target -> target.table()).distinct().sorted().toList();
        if (available == null || !available.containsAll(tables)) {
            throw new IllegalArgumentException("SHARDING_TARGET_UNAVAILABLE");
        }
        return tables;
    }
}
