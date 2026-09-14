package top.egon.cola.component.common.mybatis.sharding.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.TableProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Broadcast dictionary table. Runtime writes are rejected by the LOCAL guard profile kind.
 */
@Slf4j
@Component("BROADCAST")
@RequiredArgsConstructor
public class EgonColaBroadcastReadOnlyShardingStrategy implements EgonColaShardingStrategy {

    static final String TYPE = "BROADCAST";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void validateTable(TableProperties table) {
        EgonColaSingleTableShardingStrategy.requireType(table, TYPE);
    }

    @Override
    public String toRulesFragment(String logicalTable, TableProperties table,
                                  List<PhysicalDataSourceProperties> dataSources) {
        toRoutingProfile(logicalTable, table, dataSources);
        return "- !BROADCAST\n  tables:\n    - " + logicalTable + "\n";
    }

    @Override
    public EgonColaRoutingProfileBO toRoutingProfile(String logicalTable, TableProperties table,
                                                     List<PhysicalDataSourceProperties> dataSources) {
        validateTable(table);
        List<String> groups = EgonColaShardingStrategyNodes.primaryGroups(dataSources);
        if (groups.isEmpty()) {
            throw new EgonColaMybatisPlusConfigurationException("BROADCAST_PRIMARY_REQUIRED");
        }
        List<EgonColaPhysicalTargetBO> nodes = new ArrayList<>();
        for (String group : groups) {
            nodes.add(new EgonColaPhysicalTargetBO(group, "public", logicalTable));
        }
        return EgonColaShardingStrategyNodes.simple(logicalTable, TableKindEnum.BROADCAST_READ_ONLY, nodes);
    }
}
