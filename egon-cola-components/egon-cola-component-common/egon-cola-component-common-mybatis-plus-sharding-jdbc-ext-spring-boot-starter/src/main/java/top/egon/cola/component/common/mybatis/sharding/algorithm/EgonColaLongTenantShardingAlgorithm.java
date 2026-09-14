package top.egon.cola.component.common.mybatis.sharding.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.common.mybatis.sharding.resolver.EgonColaShardingWriteTargetResolver;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Routes a positive tenant id to one stable database group using mix64-v1.
 */
@Slf4j
public final class EgonColaLongTenantShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    private EgonColaRoutingProfileBO profile;

    @Override
    public void init(Properties properties) {
        profile = EgonColaShardingWriteTargetResolver.profile(properties);
    }

    @Override
    public String doSharding(Collection<String> available, PreciseShardingValue<Long> value) {
        if (profile == null || value == null || value.getValue() == null || value.getValue() <= 0) {
            throw new IllegalArgumentException("SHARDING_KEY");
        }
        var result = EgonColaTwoLevelRouteStrategy.routeCandidates(profile, new EgonColaRouteQuery(
                value.getLogicTableName(),
                value.getValue(),
                List.of(),
                EgonColaRouteQuery.OperationEnum.ROUTE_CANDIDATES,
                false));
        List<String> groups = result.targets().stream().map(target -> target.group()).distinct().toList();
        if (groups.size() != 1 || available == null || !available.contains(groups.getFirst())) {
            throw new IllegalArgumentException("SHARDING_TARGET_UNAVAILABLE");
        }
        return groups.getFirst();
    }

    @Override
    public Collection<String> doSharding(Collection<String> available, RangeShardingValue<Long> value) {
        throw new UnsupportedOperationException("TENANT_RANGE_FORBIDDEN");
    }
}
