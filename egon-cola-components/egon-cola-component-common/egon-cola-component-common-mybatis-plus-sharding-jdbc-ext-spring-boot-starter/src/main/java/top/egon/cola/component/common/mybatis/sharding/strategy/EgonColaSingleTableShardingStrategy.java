package top.egon.cola.component.common.mybatis.sharding.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.PartitionKeyBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO.TableKindEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.TableProperties;

import java.util.List;

/**
 * Exact single-node metadata table.
 */
@Slf4j
@Component("SINGLE")
@RequiredArgsConstructor
public class EgonColaSingleTableShardingStrategy implements EgonColaShardingStrategy {

    static final String TYPE = "SINGLE";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void validateTable(TableProperties table) {
        requireType(table, TYPE);
        if (table.dataSource() == null || table.dataSource().isBlank()) {
            throw new EgonColaMybatisPlusConfigurationException("SINGLE_NODE_REQUIRED");
        }
    }

    @Override
    public String toRulesFragment(String logicalTable, TableProperties table,
                                  List<PhysicalDataSourceProperties> dataSources) {
        EgonColaRoutingProfileBO profile = toRoutingProfile(logicalTable, table, dataSources);
        EgonColaPhysicalTargetBO node = profile.actualNodes().get(new PartitionKeyBO(0, 0)).getFirst();
        return "- !SINGLE\n  tables:\n    - " + node.group() + "." + node.schema() + "." + node.table() + "\n";
    }

    @Override
    public EgonColaRoutingProfileBO toRoutingProfile(String logicalTable, TableProperties table,
                                                     List<PhysicalDataSourceProperties> dataSources) {
        validateTable(table);
        EgonColaShardingStrategyNodes.requirePrimary(dataSources, table.dataSource());
        EgonColaPhysicalTargetBO node = new EgonColaPhysicalTargetBO(table.dataSource(), "public", logicalTable);
        return EgonColaShardingStrategyNodes.simple(logicalTable, TableKindEnum.SINGLE, List.of(node));
    }

    static void requireType(TableProperties table, String expected) {
        if (table == null || table.type() == null || !expected.equals(table.type())) {
            throw new EgonColaMybatisPlusConfigurationException("UNKNOWN_SHARDING_STRATEGY");
        }
    }
}
