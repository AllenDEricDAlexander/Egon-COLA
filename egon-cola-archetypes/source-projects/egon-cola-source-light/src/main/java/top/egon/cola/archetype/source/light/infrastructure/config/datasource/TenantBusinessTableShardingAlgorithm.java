package top.egon.cola.archetype.source.light.infrastructure.config.datasource;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/** Table candidates only; the MP SQL guard decides whether the actual operation may fan out. */
@Slf4j
public final class TenantBusinessTableShardingAlgorithm implements ComplexKeysShardingAlgorithm<Long> {
    private EgonColaRoutingProfileBO profile;

    @Override
    public void init(Properties properties) { profile = ShardingWriteTargetResolver.profile(properties); }

    @Override
    public Collection<String> doSharding(Collection<String> available, ComplexKeysShardingValue<Long> value) {
        if (profile == null || value == null || value.getColumnNameAndShardingValuesMap() == null
                || value.getColumnNameAndRangeValuesMap().containsKey("tenant_id")) {
            throw new IllegalArgumentException("TENANT_SHARDING_VALUE_REQUIRED");
        }
        Collection<Long> tenants = value.getColumnNameAndShardingValuesMap().get("tenant_id");
        if (tenants == null || tenants.size() != 1) { throw new IllegalArgumentException("EXACTLY_ONE_TENANT_REQUIRED"); }
        Collection<Long> selected = value.getColumnNameAndShardingValuesMap().get(profile.secondaryColumn());
        List<Long> roots = selected == null ? List.of() : List.copyOf(selected);
        boolean range = roots.isEmpty() && value.getColumnNameAndRangeValuesMap().containsKey(profile.secondaryColumn());
        var result = EgonColaTwoLevelRouteStrategy.routeCandidates(profile, new EgonColaRouteQuery(value.getLogicTableName(),
                tenants.iterator().next(), roots, EgonColaRouteQuery.OperationEnum.ROUTE_CANDIDATES, range));
        List<String> tables = result.targets().stream().map(target -> target.table()).distinct().sorted().toList();
        if (available == null || !available.containsAll(tables)) { throw new IllegalArgumentException("SHARDING_TARGET_UNAVAILABLE"); }
        return tables;
    }
}
